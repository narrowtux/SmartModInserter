package com.narrowtux.fmm.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MatchedVersion {


    public enum Operator {
        GREATER_THAN(">"),
        GREATER_THAN_OR_EQUALS(">="),
        EQUALS("=="),
        LESS_THAN("<"),
        LESS_THAN_OR_EQUALS("<=");

        private String value;

        Operator(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Operator ofValue(String value) {
            for (Operator op : values()) {
                if (op.getValue().equals(value)) {
                    return op;
                }
            }
            return null;
        }
    }

    private Integer major, minor, build;
    private Operator operator;

    public MatchedVersion(Integer major, Integer minor, Integer build, Operator operator) {
        this.major = major;
        this.minor = minor;
        this.build = build;
        this.operator = operator;
    }

    public Integer getMajor() {
        return major;
    }

    public Integer getMinor() {
        return minor;
    }

    public Integer getBuild() {
        return build;
    }

    public Operator getOperator() {
        return operator;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(operator.getValue());
        builder.append(' ');
        if (major != null) {
            builder.append(major);
        } else {
            builder.append("?");
        }
        if (minor != null) {
            builder.append(".");
            builder.append(minor);
        } else {
            builder.append(".?");
        }
        if (build != null) {
            builder.append(".");
            builder.append(build);
        } else {
            builder.append(".?");
        }
        return builder.toString();
    }

    public boolean matches(Version version) {
        Integer major = this.major, minor = this.minor, build = this.build;
        if (null == major) {
            major = fillInt(operator);
        }
        if (null == minor) {
            minor = fillInt(operator);
        }
        if (null == build) {
            build = fillInt(operator);
        }

        switch (operator) {
            case EQUALS:
                if (null == major || major == version.getMajor()) {
                    if (null == minor || minor == version.getMinor()) {
                        if (null == build || build == version.getBuild()) {
                            return true;
                        }
                    }
                }
                return false;
            case GREATER_THAN:
                if (version.getBuild() > build) {
                    if (version.getMinor() >= minor) {
                        return version.getMajor() >= major;
                    }
                } else {
                    if (version.getMinor() > minor) {
                        return version.getMajor() >= major;
                    }
                }
                return version.getMajor() > major;
            case GREATER_THAN_OR_EQUALS:
                return version.getMajor() >= major && version.getMinor() >= minor && version.getBuild() >= build;
            case LESS_THAN:
                if (version.getMajor() < major) {
                    return true;
                } else if (minor != null && version.getMajor() == major) {
                    if (version.getMinor() < minor) {
                        return true;
                    } else if (build != null && version.getMinor() == minor) {
                        return version.getBuild() < build;
                    }
                }
                return false;
            case LESS_THAN_OR_EQUALS:
                return version.getMajor() <= major && version.getMinor() <= minor && version.getBuild() <= build;
            default:
                throw new IllegalStateException("Unsupported operator");
        }
    }

    private static Pattern MATCHED_VERSION_PATTERN = Pattern.compile("([<>=]+)\\s?([0-9\\.]+)");

    public static MatchedVersion valueOf(String value) {
        Matcher matcher = MATCHED_VERSION_PATTERN.matcher(value);
        if (matcher.matches()) {
            Operator op = Operator.ofValue(matcher.group(1));
            if (op == null) {
                System.out.println("Invalid operator '" + matcher.group(1) + "'");
                return null;
            }
            String v = matcher.group(2);
            Integer major = null, minor = null, build = null;
            String vs[] = v.split("\\.");
            if (vs.length >= 1) {
                major = Integer.valueOf(vs[0]);
            }
            if (vs.length >= 2) {
                minor = Integer.valueOf(vs[1]);
            }
            if (vs.length >= 3) {
                build = Integer.valueOf(vs[2]);
            }
            return new MatchedVersion(major, minor, build, op);
        }
        return null;
    }

    private static Integer fillInt(Operator operator) {
        switch (operator) {
            case GREATER_THAN_OR_EQUALS:
            case GREATER_THAN:
                return -1;
            case EQUALS:
                return null; // equals to anything
            case LESS_THAN:
                return null;
            case LESS_THAN_OR_EQUALS:
                return Integer.MAX_VALUE;
            default:
                throw new IllegalStateException();
        }
    }
}
