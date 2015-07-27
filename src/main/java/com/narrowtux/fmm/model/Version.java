package com.narrowtux.fmm.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version implements Comparable<Version> {
    public static final Pattern VERSION_PATTERN = Pattern.compile("([0-9]+)\\.([0-9]+)\\.([0-9]+)");

    private int major;
    private int minor;
    private int build;

    public Version(int major, int minor, int build) {
        this.major = major;
        this.minor = minor;
        this.build = build;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getBuild() {
        return build;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Version version = (Version) o;

        if (major != version.major) return false;
        if (minor != version.minor) return false;
        return build == version.build;

    }

    @Override
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + build;
        return result;
    }

    public static Version valueOf(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (matcher.matches()) {
            return new Version(Integer.valueOf(matcher.group(1)), Integer.valueOf(matcher.group(2)), Integer.valueOf(matcher.group(3)));
        }
        return null;
    }

    @Override
    public String toString() {
        return "" + major + '.' + minor + '.' + build;
    }

    @Override
    public int compareTo(Version o) {
        if (this.major == o.major) {
            if (this.minor == o.minor) {
                return Integer.compare(this.build, o.build);
            } else {
                return Integer.compare(this.minor, o.minor);
            }
        } else {
            return Integer.compare(this.major, o.major);
        }
    }
}
