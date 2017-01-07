package org.jenkinsci.plugins.liquibase.evaluator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CredentialsMigratorTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    @LocalData
    public void should_create_credentials() {

        System.out.printf("here i am");


   }

}