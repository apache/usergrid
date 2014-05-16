/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.security.shiro;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.util.AntPathMatcher;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;


public class CustomPermission extends WildcardPermission {

    /**
     *
     */
    private static final String ME = "/me/";

    static AntPathMatcher matcher = new AntPathMatcher();

    private static final long serialVersionUID = 1L;


    public CustomPermission() {
    }


    public CustomPermission( String wildcardString ) {
        super( wildcardString );
    }


    public CustomPermission( String wildcardString, boolean caseSensitive ) {
        super( wildcardString, caseSensitive );
    }


    @Override
    public List<Set<String>> getParts() {
        return super.getParts();
    }


    @Override
    public boolean implies( Permission p ) {
        // By default only supports comparisons with other
        // PathBasedWildcardPermission
        if ( !( p instanceof CustomPermission ) ) {
            return false;
        }

        CustomPermission wp = ( CustomPermission ) p;

        List<Set<String>> otherParts = wp.getParts();

        boolean isApp = false;
        int i = 0;
        for ( Set<String> otherPart : otherParts ) {
            // If this permission has less parts than the other permission,
            // everything after the number of parts contained
            // in this permission is automatically implied, so return true
            if ( ( getParts().size() - 1 ) < i ) {
                return true;
            }
            else {
                if ( ( i == 0 ) && otherPart.contains( "applications" ) ) {
                    isApp = true;
                }
                // this part is the permission, the other part is the challenger
                Set<String> part = getParts().get( i );
                // if we know we're doing an application compare
                // then make sure all the parts from the third onwards
                // are normalized as paths
                if ( isApp && ( i > 2 ) ) {
                    part = makePaths( part );
                    otherPart = makePaths( otherPart );
                }
                if ( !part.contains( WILDCARD_TOKEN ) && !partContainsPart( part, otherPart ) ) {
                    return false;
                }
                i++;
            }
        }

        // If this permission has more parts than the other parts, only imply it
        // if all of the other parts are wildcards
        for (; i < getParts().size(); i++ ) {
            Set<String> part = getParts().get( i );
            if ( !part.contains( WILDCARD_TOKEN ) ) {
                return false;
            }
        }

        return true;
    }


    static String normalizeIfPath( String p ) {
        if ( p.startsWith( "/" ) ) {
            if ( !p.endsWith( "/" ) && !p.endsWith( "*" ) ) {
                p += "/";
            }
        }
        return p;
    }


    static String makePath( String p ) {
        if ( p.equals( "*" ) ) {
            p = "/**";
        }
        if ( !p.startsWith( "/" ) ) {
            p = "/" + p;
        }
        // if (!p.endsWith("/") && !p.endsWith("*")) {
        // p += "/";
        // }
        return p;
    }


    static Set<String> makePaths( Set<String> part ) {
        Set<String> newPart = new HashSet<String>();
        for ( String p : part ) {
            newPart.add( makePath( p ) );
        }
        return newPart;
    }


    static boolean isPath( String p ) {
        return p.contains( "/" );
    }


    private static boolean doCompare( String p1, String p2 ) {

        if ( p1.contains( "${user}" ) ) {
            UserInfo user = SubjectUtils.getUser();
            if ( user != null ) {
                if ( doCompare( p1.replace( "${user}", user.getUsername() ), p2 ) ) {
                    return true;
                }
                if ( doCompare( p1.replace( "${user}", user.getUuid().toString() ), p2 ) ) {
                    return true;
                }
            }
        }
        else if ( p1.contains( ME ) ) {
            UserInfo user = SubjectUtils.getUser();
            if ( user != null ) {
                if ( doCompare( p1.replace( ME, String.format( "/%s/", user.getUsername() ) ), p2 ) ) {
                    return true;
                }
                if ( doCompare( p1.replace( ME, String.format( "/%s/", user.getUuid().toString() ) ), p2 ) ) {
                    return true;
                }
            }
        }

        if ( isPath( p1 ) || isPath( p2 ) ) {
            p1 = makePath( p1 );
            p2 = makePath( p2 );
        }
        if ( matcher.isPattern( p1 ) ) {
            if ( matcher.match( p1, p2 ) ) {
                return true;
            }
            return matcher.match(normalizeIfPath(p1), normalizeIfPath(p2));
        }
        return p1.equalsIgnoreCase( p2 );
    }


    public static boolean partContainsPath( Set<String> part, String path ) {

        for ( String subpart : part ) {
            if ( doCompare( subpart, path ) ) {
                return true;
            }
        }

        return false;
    }


    public static boolean partContainsPart( Set<String> part, Set<String> otherPart ) {
        boolean containsAll = true;

        for ( String path : otherPart ) {
            boolean contains = false;
            for ( String subpart : part ) {
                if ( doCompare( subpart, path ) ) {
                    contains = true;
                    break;
                }
            }
            containsAll &= contains;
        }

        return containsAll;
    }
}
