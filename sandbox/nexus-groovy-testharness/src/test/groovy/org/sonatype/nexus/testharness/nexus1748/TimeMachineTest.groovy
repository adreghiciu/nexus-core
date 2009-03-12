package org.sonatype.nexus.testharness.nexus1748

import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.component.annotations.*
import org.testng.annotations.*

import org.sonatype.nexus.groovytest.NexusCompatibility
import static org.testng.Assert.*@Component(role = TimeMachineTest.class)
public class TimeMachineTest implements Contextualizable {


	@Requirement(role = ColdFusionReactor.class, hint = "java")
	def javaReactor;

	@Requirement(role = ColdFusionReactor.class, hint = "groovy")
	def groovyReactor;
	
	def context;

	@Test
    @NexusCompatibility (minVersion = "1.3")
	void testPlexusContext(){
		assertNotNull context
	}

	@Test
    @NexusCompatibility (minVersion = "1.3")
	void testPlexusJavaWiring()
	{
		assertNotNull javaReactor
		assertTrue javaReactor.givePower( 10000 );
		assertFalse javaReactor.givePower( Integer.MAX_VALUE );
	}

	@Test
    @NexusCompatibility (minVersion = "1.3")
	void testPlexusGroovyWiring()
	{
		assertNotNull groovyReactor
		assertTrue groovyReactor.givePower( 10000 );
		assertFalse groovyReactor.givePower( Integer.MAX_VALUE );
	}

	@Test(expectedExceptions = [IllegalArgumentException.class])
    @NexusCompatibility (minVersion = "1.3")
	void testJavaException()
	{
		assertTrue javaReactor.givePower( -1 );
	}

	@Test(expectedExceptions = [IllegalArgumentException.class])
    @NexusCompatibility (minVersion = "1.3")
	void testGroovyException()
	{
		assertTrue groovyReactor.givePower( -1 );
	}

	void contextualize( org.codehaus.plexus.context.Context context ) 
	{
	    this.context = context;
	}
}