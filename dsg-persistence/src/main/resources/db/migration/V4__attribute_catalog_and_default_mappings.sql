-- Attribute catalog expansion + default_attribute_mapping (wiki §1.8)

CREATE TABLE default_attribute_mapping (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    display_sequence     INT    NOT NULL,
    directory_type_id    INT    NOT NULL,
    rc_attribute_id      INT    NOT NULL,
    direction_id         INT    NOT NULL,
    directory_attribute_id INT  NOT NULL,
    CONSTRAINT fk_default_attr_map_directory_type
        FOREIGN KEY (directory_type_id) REFERENCES directory_type (id),
    CONSTRAINT fk_default_attr_map_rc_attribute
        FOREIGN KEY (rc_attribute_id) REFERENCES rc_attribute (id),
    CONSTRAINT fk_default_attr_map_direction
        FOREIGN KEY (direction_id) REFERENCES sync_direction (id),
    CONSTRAINT fk_default_attr_map_directory_attribute
        FOREIGN KEY (directory_attribute_id) REFERENCES directory_attribute (id),
    UNIQUE KEY uk_default_attr_map_sequence (directory_type_id, direction_id, display_sequence),
    KEY idx_default_attr_map_directory (directory_type_id, direction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Align RC paths with Extension API contact model
UPDATE rc_attribute SET attribute_path = 'contact.firstName', display_name = 'First Name' WHERE attribute_name = 'firstName';
UPDATE rc_attribute SET attribute_path = 'contact.lastName', display_name = 'Last Name' WHERE attribute_name = 'lastName';
UPDATE rc_attribute SET attribute_path = 'contact.email', display_name = 'Email' WHERE attribute_name = 'email';
UPDATE rc_attribute SET attribute_path = 'contact.department', display_name = 'Department' WHERE attribute_name = 'department';
UPDATE rc_attribute SET attribute_path = 'contact.jobTitle', display_name = 'Job Title' WHERE attribute_name = 'jobTitle';

INSERT INTO rc_attribute (id, attribute_name, attribute_path, display_name) VALUES
    (6, 'mobilePhone', 'contact.mobilePhone', 'Mobile Phone'),
    (7, 'street', 'contact.businessAddress.street', 'Street'),
    (8, 'city', 'contact.businessAddress.city', 'City'),
    (9, 'state', 'contact.businessAddress.state', 'State'),
    (10, 'zip', 'contact.businessAddress.zip', 'ZIP / Postal Code'),
    (11, 'country', 'contact.businessAddress.country', 'Country');

-- Canonical UI name (attribute_name) + provider fetch path (attribute_path)
UPDATE directory_attribute SET attribute_name = 'user.firstName', attribute_path = 'givenName', description = 'Azure first name' WHERE id = 1;
UPDATE directory_attribute SET attribute_name = 'user.lastName', attribute_path = 'surname', description = 'Azure last name' WHERE id = 2;
UPDATE directory_attribute SET attribute_name = 'user.email', attribute_path = 'mail', description = 'Azure email' WHERE id = 3;
UPDATE directory_attribute SET attribute_name = 'user.firstName', attribute_path = 'profile.firstName', description = 'Okta first name' WHERE id = 4;
UPDATE directory_attribute SET attribute_name = 'user.lastName', attribute_path = 'profile.lastName', description = 'Okta last name' WHERE id = 5;
UPDATE directory_attribute SET attribute_name = 'user.email', attribute_path = 'profile.email', description = 'Okta email' WHERE id = 6;
UPDATE directory_attribute SET attribute_name = 'user.firstName', attribute_path = 'name.givenName', description = 'Google first name' WHERE id = 7;
UPDATE directory_attribute SET attribute_name = 'user.lastName', attribute_path = 'name.familyName', description = 'Google last name' WHERE id = 8;
UPDATE directory_attribute SET attribute_name = 'user.email', attribute_path = 'primaryEmail', description = 'Google primary email' WHERE id = 9;

INSERT INTO directory_attribute (id, directory_type_id, attribute_name, attribute_path, description) VALUES
    (10, 1, 'user.mobilePhone', 'mobilePhone', 'Azure mobile phone'),
    (11, 1, 'user.streetAddress', 'streetAddress', 'Azure street'),
    (12, 1, 'user.city', 'city', 'Azure city'),
    (13, 1, 'user.state', 'state', 'Azure state'),
    (14, 1, 'user.zipCode', 'postalCode', 'Azure postal code'),
    (15, 1, 'user.countryCode', 'country', 'Azure country'),
    (16, 1, 'user.department', 'department', 'Azure department'),
    (17, 1, 'user.jobTitle', 'jobTitle', 'Azure job title'),
    (18, 2, 'user.mobilePhone', 'profile.mobilePhone', 'Okta mobile phone'),
    (19, 2, 'user.streetAddress', 'profile.streetAddress', 'Okta street'),
    (20, 2, 'user.city', 'profile.city', 'Okta city'),
    (21, 2, 'user.state', 'profile.state', 'Okta state'),
    (22, 2, 'user.zipCode', 'profile.zipCode', 'Okta postal code'),
    (23, 2, 'user.countryCode', 'profile.countryCode', 'Okta country'),
    (24, 2, 'user.department', 'profile.department', 'Okta department'),
    (25, 2, 'user.jobTitle', 'profile.title', 'Okta job title'),
    (26, 3, 'user.mobilePhone', 'phones.mobile', 'Google mobile (phones[type=mobile].value)'),
    (27, 3, 'user.streetAddress', 'addresses.streetAddress', 'Google street (work address)'),
    (28, 3, 'user.city', 'addresses.locality', 'Google city'),
    (29, 3, 'user.state', 'addresses.region', 'Google state'),
    (30, 3, 'user.zipCode', 'addresses.postalCode', 'Google postal code'),
    (31, 3, 'user.countryCode', 'addresses.country', 'Google country'),
    (32, 3, 'user.department', 'organizations.department', 'Google department (organizations)'),
    (33, 3, 'user.jobTitle', 'organizations.title', 'Google job title');

-- Default IDP → RC mappings (direction_id 1 = Directory to RC), display_sequence 1..10
INSERT INTO default_attribute_mapping (display_sequence, directory_type_id, rc_attribute_id, direction_id, directory_attribute_id) VALUES
    (1, 1, 1, 1, 1), (2, 1, 2, 1, 2), (3, 1, 3, 1, 3), (4, 1, 6, 1, 10), (5, 1, 7, 1, 11),
    (6, 1, 8, 1, 12), (7, 1, 9, 1, 13), (8, 1, 10, 1, 14), (9, 1, 11, 1, 15), (10, 1, 4, 1, 16),
    (1, 2, 1, 1, 4), (2, 2, 2, 1, 5), (3, 2, 3, 1, 6), (4, 2, 6, 1, 18), (5, 2, 7, 1, 19),
    (6, 2, 8, 1, 20), (7, 2, 9, 1, 21), (8, 2, 10, 1, 22), (9, 2, 11, 1, 23), (10, 2, 4, 1, 24),
    (1, 3, 1, 1, 7), (2, 3, 2, 1, 8), (3, 3, 3, 1, 9), (4, 3, 6, 1, 26), (5, 3, 7, 1, 27),
    (6, 3, 8, 1, 28), (7, 3, 9, 1, 29), (8, 3, 10, 1, 30), (9, 3, 11, 1, 31), (10, 3, 4, 1, 32);
