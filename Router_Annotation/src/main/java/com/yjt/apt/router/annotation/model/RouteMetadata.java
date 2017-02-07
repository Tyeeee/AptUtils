package com.yjt.apt.router.annotation.model;

import com.yjt.apt.router.annotation.Route;
import com.yjt.apt.router.annotation.constant.RouteType;

import java.util.Map;

import javax.lang.model.element.Element;

public class RouteMetadata {

    private RouteType type;         // Type of route
    private Element rawType;        // Raw type of route
    private Class<?> destination;   // Destination
    private String path;            // Path of route
    private String group;           // Group of route
    private int priority = -1;      // The smaller the number, the higher the priority
    private int extra;              // Extra data
    private Map<String, Integer> parametersType;  // Param type

    public RouteMetadata() { }

    public static RouteMetadata build(RouteType type, Class<?> destination, String path, String group, int priority, int extra) {
        return new RouteMetadata(type, null, destination, path, group, null, priority, extra);
    }

    public static RouteMetadata build(RouteType type, Class<?> destination, String path, String group, Map<String, Integer> paramsType, int priority, int extra) {
        return new RouteMetadata(type, null, destination, path, group, paramsType, priority, extra);
    }

    public RouteMetadata(Route route, Class<?> destination, RouteType type) {
        this(type, null, destination, route.path(), route.group(), null, route.priority(), route.extras());
    }

    public RouteMetadata(Route route, Element rawType, RouteType type, Map<String, Integer> parametersType) {
        this(type, rawType, null, route.path(), route.group(), parametersType, route.priority(), route.extras());
    }

    public RouteMetadata(RouteType type, Element rawType, Class<?> destination, String path, String group, Map<String, Integer> parametersType, int priority, int extra) {
        this.type = type;
        this.destination = destination;
        this.rawType = rawType;
        this.path = path;
        this.group = group;
        this.parametersType = parametersType;
        this.priority = priority;
        this.extra = extra;
    }

    public Map<String, Integer> getParametersType() {
        return parametersType;
    }

    public RouteMetadata setParametersType(Map<String, Integer> parametersType) {
        this.parametersType = parametersType;
        return this;
    }

    public Element getRawType() {
        return rawType;
    }

    public RouteMetadata setRawType(Element rawType) {
        this.rawType = rawType;
        return this;
    }

    public RouteType getType() {
        return type;
    }

    public RouteMetadata setType(RouteType type) {
        this.type = type;
        return this;
    }

    public Class<?> getDestination() {
        return destination;
    }

    public RouteMetadata setDestination(Class<?> destination) {
        this.destination = destination;
        return this;
    }

    public String getPath() {
        return path;
    }

    public RouteMetadata setPath(String path) {
        this.path = path;
        return this;
    }

    public String getGroup() {
        return group;
    }

    public RouteMetadata setGroup(String group) {
        this.group = group;
        return this;
    }

    public int getPriority() {
        return priority;
    }

    public RouteMetadata setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public int getExtra() {
        return extra;
    }

    public RouteMetadata setExtra(int extra) {
        this.extra = extra;
        return this;
    }

    @Override
    public String toString() {
        return "RouteMetadata{" +
                "type=" + type +
                ", rawType=" + rawType +
                ", destination=" + destination +
                ", path='" + path + '\'' +
                ", group='" + group + '\'' +
                ", priority=" + priority +
                ", extra=" + extra +
                '}';
    }
}