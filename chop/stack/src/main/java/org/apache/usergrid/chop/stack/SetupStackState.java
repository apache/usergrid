package org.apache.usergrid.chop.stack;


/**
 * Represents the setup state of a stack
 */
public enum SetupStackState {
    SetUp,
    SettingUp,
    SetupFailed,
    NotSetUp,
    Destroying,
    NotFound
}
