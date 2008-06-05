package org.sonatype.scheduling;

import java.util.HashMap;
import java.util.List;

public interface TaskConfigManager
{
    String ROLE = TaskConfigManager.class.getName();
    
    public <T> void addTask( SubmittedTask<T> task );
    public <T> void removeTask( SubmittedTask<T> task );
    public HashMap<String, List<SubmittedTask<?>>> getTasks();
}
