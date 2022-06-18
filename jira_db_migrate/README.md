# JiraDBMigrate
Migrate issues from one DB to another

===

**!! Be careful while performing DB operations in your JIRA environment !!**

**!! DB migration is not suggested !!**


If you are still here, then you are desperately looking for a solution to migrate your JIRA issues between DB's without using commercial plugins.

This is an example Java class to migrate Jira issues between DB's.
There are several assumptions and "tricks" to perform this task.
Since the mappings and some hardcoded ID's are specific to installation, you may not be able to use this code as it is. But it may give some idea about the dynamics of the JIRA workflows. 
