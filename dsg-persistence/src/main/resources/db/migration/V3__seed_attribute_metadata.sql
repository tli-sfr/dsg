-- Minimal attribute metadata for Admin API attribute-mapping (Epic 2.2)

INSERT INTO rc_attribute (id, attribute_name, attribute_path, display_name) VALUES
    (1, 'firstName', 'name.givenName', 'First Name'),
    (2, 'lastName', 'name.familyName', 'Last Name'),
    (3, 'email', 'emails[0].value', 'Email'),
    (4, 'department', 'department', 'Department'),
    (5, 'jobTitle', 'title', 'Job Title');

INSERT INTO directory_attribute (id, directory_type_id, attribute_name, attribute_path, description) VALUES
    (1, 1, 'givenName', 'givenName', 'Azure given name'),
    (2, 1, 'surname', 'surname', 'Azure surname'),
    (3, 1, 'mail', 'mail', 'Azure email'),
    (4, 2, 'firstName', 'profile.firstName', 'Okta first name'),
    (5, 2, 'lastName', 'profile.lastName', 'Okta last name'),
    (6, 2, 'email', 'profile.email', 'Okta email'),
    (7, 3, 'givenName', 'name.givenName', 'Google given name'),
    (8, 3, 'familyName', 'name.familyName', 'Google family name'),
    (9, 3, 'primaryEmail', 'primaryEmail', 'Google email');
