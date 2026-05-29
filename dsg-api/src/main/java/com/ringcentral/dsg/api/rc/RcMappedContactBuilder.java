package com.ringcentral.dsg.api.rc;

import com.ringcentral.dsg.directory.DirectoryUser;

/**
 * Builds RingCentral extension {@code contact} from attribute-mapping RC fields on {@link DirectoryUser}.
 */
public final class RcMappedContactBuilder {

    private RcMappedContactBuilder() {}

    public static RcBulkAssignContact buildContact(DirectoryUser user) {
        String firstName = requireRcAttribute(user, "firstName");
        String lastName = requireRcAttribute(user, "lastName");
        String email = requireRcAttribute(user, "email");
        String department = rcAttribute(user, "department");
        String mobilePhone = rcAttribute(user, "mobilePhone");
        RcBusinessAddress address = buildBusinessAddress(user);
        return new RcBulkAssignContact(firstName, lastName, email, department, mobilePhone, address);
    }

    private static RcBusinessAddress buildBusinessAddress(DirectoryUser user) {
        String street = rcAttribute(user, "street");
        String city = rcAttribute(user, "city");
        String zip = rcAttribute(user, "zip");
        String state = rcAttribute(user, "state");
        String country = rcAttribute(user, "country");
        if (street == null && city == null && zip == null && state == null && country == null) {
            return null;
        }
        return new RcBusinessAddress(street, city, zip, state, country);
    }

    private static String requireRcAttribute(DirectoryUser user, String rcAttributeName) {
        String value = rcAttribute(user, rcAttributeName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Required RC attribute '" + rcAttributeName + "' is missing after attribute mapping");
        }
        return value;
    }

    private static String rcAttribute(DirectoryUser user, String rcAttributeName) {
        String value = user.attributes().get(rcAttributeName);
        return value != null && !value.isBlank() ? value : null;
    }
}
