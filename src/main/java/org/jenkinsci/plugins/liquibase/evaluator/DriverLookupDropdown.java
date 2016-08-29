package org.jenkinsci.plugins.liquibase.evaluator;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

@Extension
public class DriverLookupDropdown {


    public static abstract class DriverLookup implements ExtensionPoint, Describable<DriverLookup> {
        String name;

        public DriverLookup(String name) {
            this.name = name;
        }

        public Descriptor<DriverLookup> getDescriptor() {
            return Jenkins.getInstance().getDescriptor(getClass());
        }

    }

    public static class IncludedDriver extends DriverLookup {

        public IncludedDriver(String name) {
            super(name);
        }
    }
}
