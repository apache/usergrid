# Roles & Permissions

## Roles

A role represents a set of permissions that enable certain operations to
be performed on a specific endpoint. You can assign a user to a role,
and in this way give the user the permissions associated with that role.

**Note:** The /rolenames endpoint is no longer valid. If your code currently
makes calls to /rolenames, you need to change the calls to use /roles.

## Permissions

Each role contains multiple permissions. Permissions work by whitelisting by default, meaning any permission that is not explicitely granted is denied by default. Permission is an HTTP verb (GET to allow reads, POST to allow creation, PUT to allow edits and DELETE to allow deletes) combined with a path, with optional wildcards. For example the permission put:/users/* allows editing any user.

Permissions can be added to roles, groups or to users directly, and a user’s permission is the combination of its personal permissions and the permissions of any role he’s been assigned, and the permissions of any group he’s a member of.

Permissions are only valid within the scope of a single application, so the permission paths do not need to be prefixed with /org\_name\_or\_uuid/app\_name\_or\_uuid.