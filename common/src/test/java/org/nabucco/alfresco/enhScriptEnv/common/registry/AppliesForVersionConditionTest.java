package org.nabucco.alfresco.enhScriptEnv.common.registry;

import org.alfresco.util.VersionNumber;
import org.junit.Assert;
import org.junit.Test;
import org.nabucco.alfresco.enhScriptEnv.common.script.registry.AppliesForVersionCondition;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class AppliesForVersionConditionTest
{

    @Test
    public void testFromExclusive()
    {
        final AppliesForVersionCondition condition = new AppliesForVersionCondition(new VersionNumber("1.2.3"), true);

        final DummyVersionRegisterableScript script = new DummyVersionRegisterableScript();

        script.setAppliesFromExclusive(true);
        script.setForCommunity(true);

        script.setAppliesFrom(new VersionNumber("1.2"));
        Assert.assertTrue("AppliesFromExclusive 1.2 does not match 1.2.3", condition.matches(script));
        script.setAppliesFrom(new VersionNumber("1.2.2"));
        Assert.assertTrue("AppliesFromExclusive 1.2.2 does not match 1.2.3", condition.matches(script));
        script.setAppliesFrom(new VersionNumber("1.2.2.9"));
        Assert.assertTrue("AppliesFromExclusive 1.2.2.9 does not match 1.2.3", condition.matches(script));

        script.setAppliesFrom(new VersionNumber("1.2.3"));
        Assert.assertFalse("AppliesFromExclusive 1.2.3 does match 1.2.3", condition.matches(script));
        script.setAppliesFrom(new VersionNumber("1.2.3.0"));
        Assert.assertFalse("AppliesFromExclusive 1.2.3.0 does match 1.2.3", condition.matches(script));
        script.setAppliesFrom(new VersionNumber("1.2.3.1"));
        Assert.assertFalse("AppliesFromExclusive 1.2.3.1 does match 1.2.3", condition.matches(script));
        script.setAppliesFrom(new VersionNumber("1.2.4"));
        Assert.assertFalse("AppliesFromExclusive 1.2.4 does match 1.2.3", condition.matches(script));
    }

    @Test
    public void testFromInclusive()
    {
        final AppliesForVersionCondition condition = new AppliesForVersionCondition(new VersionNumber("1.2.3"), true);

        final DummyVersionRegisterableScript script = new DummyVersionRegisterableScript();

        script.setAppliesFromExclusive(false);
        script.setForCommunity(true);

        script.setAppliesFrom(new VersionNumber("1.2"));
        Assert.assertTrue("AppliesFromInclusive 1.2 does not match 1.2.3", condition.matches(script));
        script.setAppliesFrom(new VersionNumber("1.2.2"));
        Assert.assertTrue("AppliesFromInclusive 1.2.2 does not match 1.2.3", condition.matches(script));
        script.setAppliesFrom(new VersionNumber("1.2.2.9"));
        Assert.assertTrue("AppliesFromInclusive 1.2.2.9 does not match 1.2.3", condition.matches(script));
        script.setAppliesFrom(new VersionNumber("1.2.3"));
        Assert.assertTrue("AppliesFromInclusive 1.2.3 does not match 1.2.3", condition.matches(script));
        script.setAppliesFrom(new VersionNumber("1.2.3.0"));
        Assert.assertTrue("AppliesFromInclusive 1.2.3.0 does not match 1.2.3", condition.matches(script));

        script.setAppliesFrom(new VersionNumber("1.2.3.0.1"));
        Assert.assertFalse("AppliesFromInclusive 1.2.3.0.1 does match 1.2.3", condition.matches(script));
        script.setAppliesFrom(new VersionNumber("1.2.3.1"));
        Assert.assertFalse("AppliesFromInclusive 1.2.3.1 does match 1.2.3", condition.matches(script));
        script.setAppliesFrom(new VersionNumber("1.2.4"));
        Assert.assertFalse("AppliesFromInclusive 1.2.4 does match 1.2.3", condition.matches(script));
    }

    @Test
    public void testToExclusive()
    {
        final AppliesForVersionCondition condition = new AppliesForVersionCondition(new VersionNumber("1.2.3"), true);

        final DummyVersionRegisterableScript script = new DummyVersionRegisterableScript();

        script.setAppliesToExclusive(true);
        script.setForCommunity(true);

        script.setAppliesTo(new VersionNumber("1.2"));
        Assert.assertFalse("AppliesToInclusive 1.2 does match 1.2.3", condition.matches(script));
        script.setAppliesFrom(new VersionNumber("1.2.2"));
        Assert.assertFalse("AppliesToInclusive 1.2.2 does match 1.2.3", condition.matches(script));
        script.setAppliesTo(new VersionNumber("1.2.2.9"));
        Assert.assertFalse("AppliesToInclusive 1.2.2.9 does match 1.2.3", condition.matches(script));
        script.setAppliesTo(new VersionNumber("1.2.3"));
        Assert.assertFalse("AppliesToInclusive 1.2.3 does match 1.2.3", condition.matches(script));
        script.setAppliesTo(new VersionNumber("1.2.3.0"));
        Assert.assertFalse("AppliesToInclusive 1.2.3.0 does match 1.2.3", condition.matches(script));

        script.setAppliesTo(new VersionNumber("1.2.3.0.1"));
        Assert.assertTrue("AppliesToInclusive 1.2.3.0.1 does not match 1.2.3", condition.matches(script));
        script.setAppliesTo(new VersionNumber("1.2.3.1"));
        Assert.assertTrue("AppliesToInclusive 1.2.3.1 does not match 1.2.3", condition.matches(script));
        script.setAppliesTo(new VersionNumber("1.2.4"));
        Assert.assertTrue("AppliesToInclusive 1.2.4 does not match 1.2.3", condition.matches(script));
    }

    @Test
    public void testToInclusive()
    {
        final AppliesForVersionCondition condition = new AppliesForVersionCondition(new VersionNumber("1.2.3"), true);

        final DummyVersionRegisterableScript script = new DummyVersionRegisterableScript();

        script.setAppliesToExclusive(false);
        script.setForCommunity(true);

        script.setAppliesTo(new VersionNumber("1.2"));
        Assert.assertFalse("AppliesToInclusive 1.2 does match 1.2.3", condition.matches(script));
        script.setAppliesFrom(new VersionNumber("1.2.2"));
        Assert.assertFalse("AppliesToInclusive 1.2.2 does match 1.2.3", condition.matches(script));
        script.setAppliesTo(new VersionNumber("1.2.2.9"));
        Assert.assertFalse("AppliesToInclusive 1.2.2.9 does match 1.2.3", condition.matches(script));

        script.setAppliesTo(new VersionNumber("1.2.3"));
        Assert.assertTrue("AppliesToInclusive 1.2.3 does not match 1.2.3", condition.matches(script));
        script.setAppliesTo(new VersionNumber("1.2.3.0"));
        Assert.assertTrue("AppliesToInclusive 1.2.3.0 does not match 1.2.3", condition.matches(script));
        script.setAppliesTo(new VersionNumber("1.2.3.0.1"));
        Assert.assertTrue("AppliesToInclusive 1.2.3.0.1 does not match 1.2.3", condition.matches(script));
        script.setAppliesTo(new VersionNumber("1.2.3.1"));
        Assert.assertTrue("AppliesToInclusive 1.2.3.1 does not match 1.2.3", condition.matches(script));
        script.setAppliesTo(new VersionNumber("1.2.4"));
        Assert.assertTrue("AppliesToInclusive 1.2.4 does not match 1.2.3", condition.matches(script));
    }

    @Test
    public void testToAndFromExclusive()
    {

        final AppliesForVersionCondition condition = new AppliesForVersionCondition(new VersionNumber("1.2.3"), true);

        final DummyVersionRegisterableScript script = new DummyVersionRegisterableScript();

        script.setAppliesFromExclusive(true);
        script.setAppliesToExclusive(true);
        script.setForCommunity(true);
        ;

        script.setAppliesFrom(new VersionNumber("1.1"));
        script.setAppliesTo(new VersionNumber("1.2"));

        Assert.assertFalse("AppliesTo/FromExclusive 1.1-1.2 does match 1.2.3", condition.matches(script));

        script.setAppliesTo(new VersionNumber("1.2.2.9"));
        Assert.assertFalse("AppliesTo/FromExclusive 1.1-1.2.2.9 does match 1.2.3", condition.matches(script));

        script.setAppliesTo(new VersionNumber("1.2.3"));
        Assert.assertFalse("AppliesTo/FromExclusive 1.1-1.2.3 does match 1.2.3", condition.matches(script));

        script.setAppliesTo(new VersionNumber("1.2.3.0"));
        Assert.assertFalse("AppliesTo/FromExclusive 1.1-1.2.3.0 does match 1.2.3", condition.matches(script));

        script.setAppliesTo(new VersionNumber("1.2.3.0.1"));
        Assert.assertTrue("AppliesTo/FromExclusive 1.1-1.2.3.0.1 does not match 1.2.3", condition.matches(script));

        script.setAppliesTo(new VersionNumber("1.2.3.1"));
        Assert.assertTrue("AppliesTo/FromExclusive 1.1-1.2.3.1 does not match 1.2.3", condition.matches(script));

        script.setAppliesTo(new VersionNumber("1.2.4"));
        Assert.assertTrue("AppliesTo/FromExclusive 1.1-1.2.4 does not match 1.2.3", condition.matches(script));

        script.setAppliesFrom(new VersionNumber("1.2.2.9"));
        Assert.assertTrue("AppliesTo/FromExclusive 1.2.2.9-1.2.4 does not match 1.2.3", condition.matches(script));

        script.setAppliesFrom(new VersionNumber("1.2.3"));
        Assert.assertFalse("AppliesTo/FromExclusive 1.2.3-1.2.4 does match 1.2.3", condition.matches(script));

        script.setAppliesFrom(new VersionNumber("1.2.3.0"));
        Assert.assertFalse("AppliesTo/FromExclusive 1.2.3.0-1.2.4 does match 1.2.3", condition.matches(script));

        script.setAppliesFrom(new VersionNumber("1.2.3"));
        script.setAppliesTo(new VersionNumber("1.2.3"));
        Assert.assertFalse("AppliesTo/FromExclusive 1.2.3-1.2.3 does match 1.2.3", condition.matches(script));
    }

    @Test
    public void testToAndFromInclusive()
    {

        final AppliesForVersionCondition condition = new AppliesForVersionCondition(new VersionNumber("1.2.3"), true);

        final DummyVersionRegisterableScript script = new DummyVersionRegisterableScript();

        script.setAppliesFromExclusive(false);
        script.setAppliesToExclusive(false);
        script.setForCommunity(true);

        script.setAppliesFrom(new VersionNumber("1.1"));
        script.setAppliesTo(new VersionNumber("1.2"));

        Assert.assertFalse("AppliesTo/FromExclusive 1.1-1.2 does match 1.2.3", condition.matches(script));

        script.setAppliesTo(new VersionNumber("1.2.2.9"));
        Assert.assertFalse("AppliesTo/FromExclusive 1.1-1.2.2.9 does match 1.2.3", condition.matches(script));

        script.setAppliesTo(new VersionNumber("1.2.3"));
        Assert.assertTrue("AppliesTo/FromExclusive 1.1-1.2.3 does not match 1.2.3", condition.matches(script));

        script.setAppliesTo(new VersionNumber("1.2.3.0"));
        Assert.assertTrue("AppliesTo/FromExclusive 1.1-1.2.3.0 does not match 1.2.3", condition.matches(script));

        script.setAppliesTo(new VersionNumber("1.2.3.0.1"));
        Assert.assertTrue("AppliesTo/FromExclusive 1.1-1.2.3.0.1 does not match 1.2.3", condition.matches(script));

        script.setAppliesTo(new VersionNumber("1.2.3.1"));
        Assert.assertTrue("AppliesTo/FromExclusive 1.1-1.2.3.1 does not match 1.2.3", condition.matches(script));

        script.setAppliesTo(new VersionNumber("1.2.4"));
        Assert.assertTrue("AppliesTo/FromExclusive 1.1-1.2.4 does not match 1.2.3", condition.matches(script));

        script.setAppliesFrom(new VersionNumber("1.2.2.9"));
        Assert.assertTrue("AppliesTo/FromExclusive 1.2.2.9-1.2.4 does not match 1.2.3", condition.matches(script));

        script.setAppliesFrom(new VersionNumber("1.2.3"));
        Assert.assertTrue("AppliesTo/FromExclusive 1.2.3-1.2.4 does not match 1.2.3", condition.matches(script));

        script.setAppliesFrom(new VersionNumber("1.2.3.0"));
        Assert.assertTrue("AppliesTo/FromExclusive 1.2.3.0-1.2.4 does not match 1.2.3", condition.matches(script));

        script.setAppliesFrom(new VersionNumber("1.2.3.0.1"));
        Assert.assertFalse("AppliesTo/FromExclusive 1.2.3.0.1-1.2.4 does match 1.2.3", condition.matches(script));

        script.setAppliesFrom(new VersionNumber("1.2.3.1"));
        Assert.assertFalse("AppliesTo/FromExclusive 1.2.3.1-1.2.4 does match 1.2.3", condition.matches(script));

        script.setAppliesFrom(new VersionNumber("1.2.3"));
        script.setAppliesTo(new VersionNumber("1.2.3"));
        Assert.assertTrue("AppliesTo/FromExclusive 1.2.3-1.2.3 does not match 1.2.3", condition.matches(script));
    }
}
