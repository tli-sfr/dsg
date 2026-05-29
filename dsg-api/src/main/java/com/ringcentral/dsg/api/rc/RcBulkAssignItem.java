package com.ringcentral.dsg.api.rc;

/** One entry in {@code items[]}; {@code references} live inside {@link #extension()}, not here. */
public record RcBulkAssignItem(RcBulkAssignExtension extension) {
}
