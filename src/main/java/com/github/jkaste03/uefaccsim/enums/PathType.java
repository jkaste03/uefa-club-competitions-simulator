package com.github.jkaste03.uefaccsim.enums;

/**
 * Enum representing different path types in UEFA competitions.
 */
public enum PathType {
    CHAMPIONS_PATH("Champions Path"),
    LEAGUE_PATH("League Path"),
    MAIN_PATH("Main Path");

    /**
     * The full name of the path associated with the enum constant.
     */
    private final String pathName;

    /**
     * Constructor to initialize the enum constant with the path's full name.
     *
     * @param pathName the full name of the path
     */
    PathType(String pathName) {
        this.pathName = pathName;
    }

    /**
     * Gets the full name of the path.
     *
     * @return the full name of the path
     */
    public String getPathName() {
        return pathName;
    }

    @Override
    public String toString() {
        return pathName;
    }
}
