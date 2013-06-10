/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.persistence.query.ir;

/**
 * Intermediate represnetation of the within operator
 * 
 * @author tnine
 * 
 */
public class WithinNode extends QueryNode {

	private final String propertyName;
	private final float distance;
	private final float lattitude;
	private final float longitude;
	private final QuerySlice slice;

	/**
	 * @param propertyName
	 * @param distance
	 * @param lattitude
	 * @param longitude
	 */
	public WithinNode(String propertyName, float distance, float lattitude,
			float longitude, int nodeId) {
		this.propertyName = propertyName;
		this.distance = distance;
		this.lattitude = lattitude;
		this.longitude = longitude;
		this.slice = new QuerySlice("location", nodeId);
	}

	/**
	 * @return the propertyName
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * @return the distance
	 */
	public float getDistance() {
		return distance;
	}

	/**
	 * @return the lattitude
	 */
	public float getLattitude() {
		return lattitude;
	}

	/**
	 * @return the longitude
	 */
	public float getLongitude() {
		return longitude;
	}

	
	/**
   * @return the slice
   */
  public QuerySlice getSlice() {
    return slice;
  }

  /*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.usergrid.persistence.query.ir.QueryNode#visit(org.usergrid.persistence
	 * .query.ir.NodeVisitor)
	 */
	@Override
	public void visit(NodeVisitor visitor) throws Exception {
		visitor.visit(this);
	}

	@Override
	public String toString() {
		return "WithinNode [propertyName=" + propertyName + ", distance="
				+ distance + ", lattitude=" + lattitude + ", longitude="
				+ longitude + "]";
	}

}
