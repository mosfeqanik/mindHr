package com.mindhr.entity.enums;

/** Which rows a role may see, ordered from widest to narrowest. */
public enum DataScope {
    PLATFORM,
    ORGANIZATION,
    BRANCH,
    TEAM,
    SELF;

    /** The wider (less restrictive) of the two scopes. */
    public DataScope widest(DataScope other) {
        return this.ordinal() <= other.ordinal() ? this : other;
    }
}
