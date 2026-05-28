-- Reference / lookup seed data for DSB (Epic 1)

INSERT INTO directory_type (id, directory_type, description) VALUES
    (1, 'Azure', 'Microsoft Entra ID / Azure AD'),
    (2, 'Okta', 'Okta Workforce Identity'),
    (3, 'Google', 'Google Workspace'),
    (4, 'OneLogin', 'OneLogin');

INSERT INTO sync_direction (id, description) VALUES
    (1, 'Directory to RC'),
    (2, 'RC to Directory');

INSERT INTO license_type (id, type, description) VALUES
    (1, 'PRIMARY_LICENSE', 'Primary user license'),
    (2, 'ADD_ON_LICENSE', 'Add-on license');

INSERT INTO dl_area_code_type (id, area_code_rule_type, description) VALUES
    (1, 'ADDRESS', 'Derive area code from user address'),
    (2, 'SPECIFIED_AREA_CODE', 'Use specified area code list');

INSERT INTO template_type (id, type, description) VALUES
    (1, 'USER', 'User template'),
    (2, 'CALL_HANDLING', 'Call handling template');

INSERT INTO device_type (id, type, description) VALUES
    (1, 'BYOD', 'Bring your own device'),
    (2, 'RINGCENTRAL_APP', 'RingCentral App'),
    (3, 'INVENTORY_PHONE', 'Inventory phone');

INSERT INTO deprovisioning_type (id, type, description) VALUES
    (1, 'FULL_DELETE', 'Option A — full delete'),
    (2, 'DISABLE_ONLY', 'Option C — disable only'),
    (3, 'RECLAIM_RESOURCE', 'Option B — reclaim resources');

INSERT INTO job_type (id, type) VALUES
    (1, 'FULL'),
    (2, 'INCREMENTAL'),
    (3, 'ON_DEMAND');

INSERT INTO job_state (id, state) VALUES
    (1, 'PENDING'),
    (2, 'IN_PREP'),
    (3, 'READY'),
    (4, 'IN_SYNC'),
    (5, 'COMPLETED'),
    (6, 'CANCELLED'),
    (7, 'CANCELLING'),
    (8, 'SUCCEEDED'),
    (9, 'FAILED'),
    (10, 'STUCK');

INSERT INTO operation_type (id, type) VALUES
    (1, 'CREATE'),
    (2, 'UPDATE'),
    (3, 'DELETE');

INSERT INTO job_frequency_type (id, frequency_type, frequency_display_name) VALUES
    (1, 'DAILY', 'Daily'),
    (2, 'WEEKLY', 'Weekly'),
    (3, 'MONTHLY', 'Monthly');

INSERT INTO rc_rule_based_attribute (id, attribute_name, display_name) VALUES
    (1, 'ROLE', 'Role'),
    (2, 'SITE', 'Site'),
    (3, 'COST_CENTER', 'Cost Center'),
    (4, 'USER_GROUP', 'User Group');
