package com.example.jiramigrate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class Migrate {
    public static Logger logger = Logger.getLogger("Migrate");

    @Autowired
    @Qualifier("mssqlJdbcTemplate")
    private JdbcTemplate mssqlTemplate;

    @Autowired
    @Qualifier("mysqlJdbcTemplate")
    private NamedParameterJdbcTemplate mysqlTemplate;

    @Autowired
    @Qualifier("mysqlTransactionTemplate")
    private TransactionTemplate mysqlTransactionTemplate;

    public void migrate(String[] issueList) {

        for (String issue : issueList) {

            List<Map<String, Object>> testMap = mysqlTemplate.getJdbcTemplate().queryForList(
                    "SELECT ID FROM jiraissue j  WHERE project=10001 AND issuenum=" + issue);
            if (testMap != null && testMap.size() > 0) {
                logger.warning("Issue " + issue + " already exists!");
                continue;
            }

            logger.info("Processing " + issue);
            List<Map<String, Object>> oldIssueMap = mssqlTemplate.queryForList(
                    "select " +
                            "   ID,   " +
                            "   ISSUENUM," +
                            "   (select lower_user_name from jiradbschema.app_user au where au.user_key=creator) CREATOR," +
                            "   (select lower_user_name from jiradbschema.app_user au where au.user_key=reporter) REPORTER," +
                            "   (select lower_user_name from jiradbschema.app_user au where au.user_key=assignee) ASSIGNEE," +
                            "   ISSUETYPE," +
                            "   SUMMARY," +
                            "   DESCRIPTION," +
                            "   PRIORITY," +
                            "   RESOLUTION," +
                            "   ISSUESTATUS," +
                            "   CONVERT(varchar, created, 120) CREATED," +
                            "   CONVERT(varchar, updated, 120) UPDATED," +
                            "   CONVERT(varchar, duedate, 120) DUEDATE," +
                            "   CONVERT(varchar, RESOLUTIONDATE, 120) RESOLUTIONDATE," +
                            "   TIMEORIGINALESTIMATE," +
                            "   TIMEESTIMATE," +
                            "   TIMESPENT," +
                            "   WATCHES " +
                            "from " +
                            "   jiradbschema.jiraissue j  " +
                            "where " +
                            "   project=10000 " +
                            "   and issuenum=" + issue);
            if (oldIssueMap == null || oldIssueMap.size() == 0) {
                logger.warning("Issue " + issue + " not found!");
                continue;
            }
            logger.info(oldIssueMap.toString());

            String oldIssueId = oldIssueMap.get(0).get("ID").toString();

            List<Map<String, Object>> cfMap = mssqlTemplate.queryForList(
                    "select cfid, cftype, cfname,  cfval, valtype from vw_cf where issueid=" + oldIssueId
            );
            logger.info(cfMap.toString());


            List<Map<String, Object>> worklogMap = mssqlTemplate.queryForList(
                    "select " +
                            "   (select lower_user_name from jiradbschema.app_user au where au.user_key=author) AUTHOR," +
                            "   WORKLOGBODY, " +
                            "   CONVERT(varchar, Created, 120) CREATED, " +
                            "   (select lower_user_name from jiradbschema.app_user au where au.user_key=updateauthor) UPDATEAUTHOR, " +
                            "   CONVERT(varchar, updated, 120) UPDATED, " +
                            "   CONVERT(varchar, startdate, 120) STARTDATE, " +
                            "   TIMEWORKED " +
                            "from " +
                            "   jiradbschema.worklog w " +
                            "WHERE " +
                            "   issueid = " + oldIssueId);
            logger.info(worklogMap.toString());

            List<Map<String, Object>> actionMap = mssqlTemplate.queryForList(
                    "select " +
                            "        (select lower_user_name from jiradbschema.app_user au where au.user_key=author) AUTHOR," +
                            "        ACTIONTYPE," +
                            "        ACTIONBODY," +
                            "        CONVERT(varchar, Created, 120) CREATED," +
                            "        (select lower_user_name from jiradbschema.app_user au where au.user_key=updateauthor) UPDATEAUTHOR," +
                            "        CONVERT(varchar, updated, 120) UPDATED," +
                            "        ACTIONNUM " +
                            "from" +
                            "   jiradbschema.jiraaction j " +
                            "where issueid= " + oldIssueId);
            logger.info(actionMap.toString());

            List<Map<String, Object>> attachmentMap = mssqlTemplate.queryForList(
                    "select  ID, " +
                            "    (select lower_user_name from jiradbschema.app_user au where au.user_key=author) AUTHOR, " +
                            "    CONVERT(varchar, Created, 120) CREATED, " +
                            "    FILENAME, " +
                            "    FILESIZE, " +
                            "    ZIP, " +
                            "    MIMETYPE, " +
                            "    THUMBNAILABLE " +
                            "from " +
                            "   jiradbschema.fileattachment f " +
                            "where " +
                            "       issueid = " + oldIssueId);
            logger.info(attachmentMap.toString());

            List<Map<String, Object>> uaMap = mssqlTemplate.queryForList(
                    "select   " +
                            "    (select lower_user_name from jiradbschema.app_user au where au.user_key=SOURCE_NAME) SOURCE_NAME, " +
                            "    CONVERT(varchar, Created, 120) CREATED, " +
                            "    SINK_NODE_ENTITY, " +
                            "    ASSOCIATION_TYPE, " +
                            "    SEQUENCE " +
                            "from " +
                            "   jiradbschema.userassociation f " +
                            "where " +
                            "       sink_node_id = " + oldIssueId);
            logger.info(uaMap.toString());

            List<Map<String, Object>> labelMap = mssqlTemplate.queryForList(
                    "select LABEL from jiradbschema.label l where ISSUE = " + oldIssueId);
            logger.info(labelMap.toString());

            List<Map<String, Object>> changeGroupMap = mssqlTemplate.queryForList(
                    "select " +
                            "   ID, " +
                            "   (select lower_user_name from jiradbschema.app_user au where au.user_key=author) AUTHOR, " +
                            "   CONVERT(varchar, Created, 120) CREATED " +
                            "from " +
                            "   jiradbschema.changegroup c " +
                            "where " +
                            "   issueid=" + oldIssueId);

            for (Map<String, Object> cg : changeGroupMap) {
                List<Map<String, Object>> changeMap = mssqlTemplate.queryForList(
                        "select " +
                                "   GROUPID, " +
                                "   FIELDTYPE, " +
                                "   FIELD, " +
                                "   OLDVALUE, " +
                                "   OLDSTRING, " +
                                "   NEWVALUE, " +
                                "   NEWSTRING " +
                                "from " +
                                "   jiradbschema.changeitem c " +
                                "where " +
                                "   groupid=" + cg.get("ID"));
                cg.put("changeMap", changeMap);
                logger.info(cg.toString() + " =>" + changeMap.toString());
            }

            // START INGESTION
            mysqlTransactionTemplate.execute(new TransactionCallback<Object>() {
                @Override
                public Object doInTransaction(TransactionStatus status) {

                    HashMap<String, Object> vals = new HashMap<>();
                    vals.putAll(oldIssueMap.get(0));
                    vals.put("PROJECT", "10001");
                    vals.put("ARCHIVED", "N");
                    vals.put("ISSUETYPE", issuetypeMappings.get(oldIssueMap.get(0).get("ISSUETYPE")));
                    vals.put("RESOLUTION", resolutionMappings.get(oldIssueMap.get(0).get("RESOLUTION")));
                    vals.put("ISSUESTATUS", statusMappings.get(oldIssueMap.get(0).get("ISSUESTATUS")));
                    vals.put("PRIORITY", priorityMappings.get(oldIssueMap.get(0).get("PRIORITY")));
                    vals.put("VOTES", "0");

                    mysqlTemplate.update(
                            "INSERT INTO " +
                                    "   jiraissue (ID, issuenum, PROJECT, REPORTER, ASSIGNEE, CREATOR, ISSUETYPE, SUMMARY, DESCRIPTION, PRIORITY, " +
                                    "          RESOLUTION, ISSUESTATUS, CREATED, UPDATED, DUEDATE, RESOLUTIONDATE, WATCHES, TIMEORIGINALESTIMATE, " +
                                    "          TIMEESTIMATE, TIMESPENT, ARCHIVED, WORKFLOW_ID)  " +
                                    "VALUES (" +
                                    "   (select max(idx)  from (select (id-1) idx from jiraissue) as tmp where not EXISTS (select 1 from jiraissue j2 where j2.id=tmp.idx)), " +
                                    "   :ISSUENUM, " +
                                    "   :PROJECT, " +
                                    "   (select user_key from app_user where lower_user_name = :REPORTER), " +
                                    "   (select user_key from app_user where lower_user_name = :ASSIGNEE), " +
                                    "   (select user_key from app_user where lower_user_name = :CREATOR), " +
                                    "   :ISSUETYPE, " +
                                    "   :SUMMARY, " +
                                    "   :DESCRIPTION, " +
                                    "   :PRIORITY, " +
                                    "   :RESOLUTION, " +
                                    "   :ISSUESTATUS, " +
                                    "   STR_TO_DATE(:CREATED, '%Y-%m-%d %H:%i:%s'), " +
                                    "   STR_TO_DATE(:UPDATED, '%Y-%m-%d %H:%i:%s'), " +
                                    "   STR_TO_DATE(:DUEDATE, '%Y-%m-%d %H:%i:%s'), " +
                                    "   STR_TO_DATE(:RESOLUTIONDATE, '%Y-%m-%d %H:%i:%s'), " +
                                    "   :WATCHES, " +
                                    "   :TIMEORIGINALESTIMATE, " +
                                    "   :TIMEESTIMATE, " +
                                    "   :TIMESPENT, " +
                                    "   :ARCHIVED," +
                                    "   (select max(idx) from (select (workflow_id-1) idx from jiraissue) as tmp where not EXISTS (select 1 from jiraissue j2 where j2.workflow_id=tmp.idx) and not EXISTS (select 1 from OS_WFENTRY j2 where j2.id=tmp.idx)) " +
                                    ") ",
                            vals);


                    List<Map<String, Object>> newIssueMap = mysqlTemplate.getJdbcTemplate().queryForList(
                            "SELECT ID, WORKFLOW_ID FROM jiraissue j  WHERE project=10001 AND issuenum=" + issue);
                    String newIssueId = newIssueMap.get(0).get("ID").toString();


                    // MUST INSERT OS_ TABLES TO CONTINUE WORKFLOW
                    mysqlTemplate.getJdbcTemplate().update(
                            "INSERT INTO " +
                                    "   OS_WFENTRY (id, name, state) " +
                                    "VALUES (" +
                                    "   " + newIssueMap.get(0).get("WORKFLOW_ID") + "," +
                                    "   '" + wfMappings.get(issuetypeMappings.get(oldIssueMap.get(0).get("ISSUETYPE"))) + "'," +
                                    "  1" +
                                    ")");

                    mysqlTemplate.update(
                            "INSERT INTO " +
                                    "   OS_CURRENTSTEP (ID, ENTRY_ID, START_DATE, STEP_ID, ACTION_ID) " +
                                    "VALUES (" +
                                    "   (select max(idx) from (select (id-1) idx from OS_CURRENTSTEP) as tmp where not EXISTS (select 1 from OS_CURRENTSTEP j2 where j2.id=tmp.idx) and not EXISTS (select 1 from OS_HISTORYSTEP_PREV j2 where j2.id=tmp.idx) and not EXISTS (select 1 from OS_HISTORYSTEP j2 where j2.id=tmp.idx)), " +
                                    "   " + newIssueMap.get(0).get("WORKFLOW_ID") + "," +
                                    "   SYSDATE()," +
                                    "   (select distinct c.STEP_ID from jiraissue j, OS_CURRENTSTEP c where j.WORKFLOW_ID = c.ENTRY_ID and j.issuetype=:ISSUETYPE and j.issuestatus=:ISSUESTATUS), " +
                                    "    0" +
                                    ")", vals);
                    ////


                    for (Map<String, Object> cf : cfMap) {
                        processCf(cf, newIssueId, issuetypeMappings.get(oldIssueMap.get(0).get("ISSUETYPE")));
                    }

                    for (Map<String, Object> wl : worklogMap) {
                        vals = new HashMap<>();
                        vals.putAll(wl);
                        mysqlTemplate.update(
                                "INSERT INTO " +
                                        "   worklog(ID, ISSUEID, AUTHOR, WORKLOGBODY, CREATED, UPDATEAUTHOR, UPDATED, STARTDATE, TIMEWORKED) " +
                                        "VALUES (" +
                                        "   (select max(idx)  from (select (id-1) idx from worklog) as tmp where not EXISTS (select 1 from worklog j2 where j2.id=tmp.idx)), " +
                                        "   " + newIssueId + "," +
                                        "   (select user_key from app_user where lower_user_name = :AUTHOR), " +
                                        "   :WORKLOGBODY, " +
                                        "   STR_TO_DATE(:CREATED, '%Y-%m-%d %H:%i:%s'), " +
                                        "   (select user_key from app_user where lower_user_name = :UPDATEAUTHOR), " +
                                        "   STR_TO_DATE(:UPDATED, '%Y-%m-%d %H:%i:%s'), " +
                                        "   STR_TO_DATE(:STARTDATE, '%Y-%m-%d %H:%i:%s'), " +
                                        "   :TIMEWORKED" +
                                        ")", vals);
                    }

                    for (Map<String, Object> ac : actionMap) {
                        vals = new HashMap<>();
                        vals.putAll(ac);
                        mysqlTemplate.update(
                                "INSERT INTO " +
                                        "   jiraaction(ID, ISSUEID, AUTHOR, ACTIONTYPE, ACTIONBODY, CREATED, UPDATEAUTHOR, UPDATED, ACTIONNUM) " +
                                        "VALUES (" +
                                        "   (select max(idx)  from (select (id-1) idx from jiraaction) as tmp where not EXISTS (select 1 from jiraaction j2 where j2.id=tmp.idx)), " +
                                        "   " + newIssueId + "," +
                                        "   (select user_key from app_user where lower_user_name = :AUTHOR), " +
                                        "   :ACTIONTYPE, " +
                                        "   :ACTIONBODY, " +
                                        "   STR_TO_DATE(:CREATED, '%Y-%m-%d %H:%i:%s'), " +
                                        "   (select user_key from app_user where lower_user_name = :UPDATEAUTHOR), " +
                                        "   STR_TO_DATE(:UPDATED, '%Y-%m-%d %H:%i:%s'), " +
                                        "   :ACTIONNUM" +
                                        ")", vals);
                    }

                    for (Map<String, Object> at : attachmentMap) {
                        vals = new HashMap<>();
                        vals.putAll(at);
                        mysqlTemplate.update(
                                "INSERT INTO " +
                                        "   fileattachment(ID, ISSUEID, AUTHOR, CREATED, FILENAME, FILESIZE, ZIP, MIMETYPE, THUMBNAILABLE) " +
                                        "VALUES (" +
                                        "   :ID, " +
                                        "   " + newIssueId + "," +
                                        "   (select user_key from app_user where lower_user_name = :AUTHOR), " +
                                        "   STR_TO_DATE(:CREATED, '%Y-%m-%d %H:%i:%s'), " +
                                        "   :FILENAME, " +
                                        "   :FILESIZE, " +
                                        "   :ZIP, " +
                                        "   :MIMETYPE, " +
                                        "   :THUMBNAILABLE" +
                                        ")", vals);
                    }

                    for (Map<String, Object> ua : uaMap) {
                        vals = new HashMap<>();
                        vals.putAll(ua);
                        mysqlTemplate.update(
                                "INSERT INTO " +
                                        "   userassociation(SOURCE_NAME, SINK_NODE_ID, CREATED, SINK_NODE_ENTITY, ASSOCIATION_TYPE, SEQUENCE) " +
                                        "VALUES (" +
                                        "   (select user_key from app_user where lower_user_name = :SOURCE_NAME), " +
                                        "   " + newIssueId + "," +
                                        "   STR_TO_DATE(:CREATED, '%Y-%m-%d %H:%i:%s'), " +
                                        "   :SINK_NODE_ENTITY, " +
                                        "   :ASSOCIATION_TYPE, " +
                                        "   :SEQUENCE" +
                                        ")", vals);
                    }

                    for (Map<String, Object> lb : labelMap) {
                        vals = new HashMap<>();
                        vals.putAll(lb);
                        mysqlTemplate.update("INSERT INTO label(ID, ISSUE, LABEL) VALUES (" +
                                "   (select max(idx)  from (select (id-1) idx from label) as tmp where not EXISTS (select 1 from label j2 where j2.id=tmp.idx)), " +
                                "   " + newIssueId + "," +
                                "   :LABEL)", vals);
                    }

                    for (Map<String, Object> cg : changeGroupMap) {
                        for (Map<String, Object> ci : ((List<Map<String, Object>>) cg.get("changeMap"))) {
                            vals = new HashMap<>();
                            vals.putAll(ci);
                            mysqlTemplate.update("INSERT INTO " +
                                    "   changeitem (ID, GROUPID, FIELDTYPE, FIELD, OLDVALUE, OLDSTRING, NEWVALUE, NEWSTRING)" +
                                    "VALUES (" +
                                    "   (select max(idx)  from (select (id-1) idx from changeitem) as tmp where not EXISTS (select 1 from changeitem j2 where j2.id=tmp.idx)), " +
                                    "   (select max(idx)  from (select (id-1) idx from changegroup) as tmp where not EXISTS (select 1 from changegroup j2 where j2.id=tmp.idx)), " +
                                    "   :FIELDTYPE, " +
                                    "   :FIELD, " +
                                    "   :OLDVALUE, " +
                                    "   :OLDSTRING, " +
                                    "   :NEWVALUE, " +
                                    "   :NEWSTRING " +
                                    ")", vals);
                        }
                        vals = new HashMap<>();
                        vals.putAll(cg);
                        mysqlTemplate.update(
                                "INSERT INTO " +
                                        "   changegroup(ID, ISSUEID, AUTHOR, CREATED) " +
                                        "VALUES (" +
                                        "   (select max(idx)  from (select (id-1) idx from changegroup) as tmp where not EXISTS (select 1 from changegroup j2 where j2.id=tmp.idx)), " +
                                        "   " + newIssueId + "," +
                                        "   (select user_key from app_user where lower_user_name = :AUTHOR), " +
                                        "   STR_TO_DATE(:CREATED, '%Y-%m-%d %H:%i:%s') " +
                                        ")", vals);
                    }
                    return null;
                }
            });

        }
    }

    void processCf(Map<String, Object> cf, String issueId, String newIssueType) {
        if (!cfMappings.containsKey(cf.get("cfid").toString()))
            return;
        HashMap<String, Object> vals = new HashMap<>();
        vals.put("ISSUE", issueId);
        vals.put("CUSTOMFIELD", cfMappings.get(cf.get("cfid").toString()).toString());
        vals.put("STRINGVALUE", cf.get("valtype").equals("stringvalue") ? cf.get("cfval") : null);
        vals.put("NUMBERVALUE", cf.get("valtype").equals("numbervalue") ? cf.get("cfval") : null);
        vals.put("TEXTVALUE", cf.get("valtype").equals("textvalue") ? cf.get("cfval") : null);
        vals.put("DATEVALUE", cf.get("valtype").equals("datevalue") ? cf.get("cfval") : null);
        String strSTRINGVALUE = ":STRINGVALUE";
        String strNUMBERVALUE = ":NUMBERVALUE";
        String strTEXTVALUE = ":TEXTVALUE";
        String strDATEVALUE = ":DATEVALUE";
        if (!cf.get("cfid").toString().equals("10937") &&
                (cf.get("cftype").toString().endsWith(":select") || cf.get("cftype").toString().endsWith(":radiobuttons"))) {
            if (cf.get("valtype").equals("stringvalue"))
                strSTRINGVALUE = "(select id from customfieldoption cfo where customvalue=:STRINGVALUE and CUSTOMFIELD=:CUSTOMFIELD)";
            if (cf.get("valtype").equals("numbervalue"))
                strNUMBERVALUE = "(select id from customfieldoption cfo where customvalue=:NUMBERVALUE and CUSTOMFIELD=:CUSTOMFIELD)";
            if (cf.get("valtype").equals("textvalue"))
                strTEXTVALUE = "(select id from customfieldoption cfo where customvalue=:TEXTVALUE and CUSTOMFIELD=:CUSTOMFIELD)";
            if (cf.get("valtype").equals("datevalue"))
                strDATEVALUE = "(select id from customfieldoption cfo where customvalue=:DATEVALUE and CUSTOMFIELD=:CUSTOMFIELD)";
        }
        if (cf.get("cftype").toString().endsWith("userpicker") || cf.get("cftype").toString().endsWith(":sd-request-participants")) {
            if (cf.get("valtype").equals("stringvalue"))
                strSTRINGVALUE = "(select user_key from app_user where lower_user_name=:STRINGVALUE)";
        }

        if (cf.get("cfid").toString().equals("10001")) {
            vals.put("STRINGVALUE", "bc/" + sdMappings.get(newIssueType));
        }

        mysqlTemplate.update(
                "INSERT INTO " +
                        " customfieldvalue (ID, ISSUE, CUSTOMFIELD, UPDATED,  STRINGVALUE, NUMBERVALUE, TEXTVALUE, DATEVALUE)" +
                        " VALUES(" +
                        "       (select max(idx)  from (select (id-1) idx from customfieldvalue) as tmp where not EXISTS (select 1 from customfieldvalue j2 where j2.id=tmp.idx)), " +
                        "       :ISSUE, " +
                        "       :CUSTOMFIELD, " +
                        "       SYSDATE(), " +
                        strSTRINGVALUE + ", " +
                        strNUMBERVALUE + ", " +
                        strTEXTVALUE + ", " +
                        strDATEVALUE +
                        ")", vals);
    }


    static HashMap<String, String> priorityMappings = new HashMap<>();
    static HashMap<String, String> statusMappings = new HashMap<>();
    static HashMap<String, String> resolutionMappings = new HashMap<>();
    static HashMap<String, String> issuetypeMappings = new HashMap<>();
    static HashMap<String, String> cfMappings = new HashMap<>();
    static HashMap<String, String> sdMappings = new HashMap<>();
    static HashMap<String, String> wfMappings = new HashMap<>();

    static {
        issuetypeMappings.put("10000", "10015");
        issuetypeMappings.put("10001", "10005");
        issuetypeMappings.put("10002", "10006");
        issuetypeMappings.put("10003", "10002");
        issuetypeMappings.put("10004", "10003");
        issuetypeMappings.put("10006", "10010");
        issuetypeMappings.put("10007", "10015");
        issuetypeMappings.put("10008", "10009");
        issuetypeMappings.put("10009", "10009");
        issuetypeMappings.put("10014", "10009");
        issuetypeMappings.put("10101", "10100");
        issuetypeMappings.put("10200", "10010");
        issuetypeMappings.put("10201", "10007");
        issuetypeMappings.put("10300", "10013");
        issuetypeMappings.put("10400", "10000");
        issuetypeMappings.put("10401", "10001");
        issuetypeMappings.put("10402", "10014");
        issuetypeMappings.put("10500", "10002");
        issuetypeMappings.put("10600", "10003");
        issuetypeMappings.put("10700", "10002");
        issuetypeMappings.put("10800", "10009");
        issuetypeMappings.put("10801", "10009");
        issuetypeMappings.put("10900", "10015");
        issuetypeMappings.put("11000", "10009");
        issuetypeMappings.put("11001", "10009");

        priorityMappings.put("1", "1");
        priorityMappings.put("10100", "10100");
        priorityMappings.put("10300", "10000");
        priorityMappings.put("10301", "10001");
        priorityMappings.put("2", "2");
        priorityMappings.put("3", "3");
        priorityMappings.put("4", "4");
        priorityMappings.put("5", "5");
        priorityMappings.put("", "");

        resolutionMappings.put("10000", "10000");
        resolutionMappings.put("10001", "10005");
        resolutionMappings.put("10002", "10002");
        resolutionMappings.put("10003", "10003");
        resolutionMappings.put("10200", "10001");
        resolutionMappings.put("10202", "10004");

        statusMappings.put("1", "10020");
        statusMappings.put("10000", "10022");
        statusMappings.put("10001", "10020");
        statusMappings.put("10002", "10100");
        statusMappings.put("10003", "10020");
        statusMappings.put("10004", "10054");
        statusMappings.put("10005", "10020");
        statusMappings.put("10006", "10020");
        statusMappings.put("10007", "10032");
        statusMappings.put("10008", "5");
        statusMappings.put("10009", "10200");
        statusMappings.put("10010", "10200");
        statusMappings.put("10011", "10020");
        statusMappings.put("10012", "10028");
        statusMappings.put("10013", "10020");
        statusMappings.put("10014", "10032");
        statusMappings.put("10015", "10032");
        statusMappings.put("10016", "10032");
        statusMappings.put("10017", "10032");
        statusMappings.put("10018", "10033");
        statusMappings.put("10019", "10020");
        statusMappings.put("10020", "10035");
        statusMappings.put("10021", "10020");
        statusMappings.put("10022", "10054");
        statusMappings.put("10023", "10020");
        statusMappings.put("10024", "10032");
        statusMappings.put("10025", "10032");
        statusMappings.put("10100", "10027");
        statusMappings.put("10200", "10032");
        statusMappings.put("10201", "10032");
        statusMappings.put("10202", "10041");
        statusMappings.put("10203", "10038");
        statusMappings.put("10204", "10038");
        statusMappings.put("10205", "10038");
        statusMappings.put("10206", "10038");
        statusMappings.put("10207", "10020");
        statusMappings.put("10208", "10020");
        statusMappings.put("10209", "10032");
        statusMappings.put("10210", "10032");
        statusMappings.put("10211", "10032");
        statusMappings.put("10300", "10020");
        statusMappings.put("10301", "10020");
        statusMappings.put("10302", "10020");
        statusMappings.put("10303", "10020");
        statusMappings.put("10304", "10020");
        statusMappings.put("10305", "10020");
        statusMappings.put("10306", "10020");
        statusMappings.put("10307", "6");
        statusMappings.put("10308", "10020");
        statusMappings.put("10309", "10020");
        statusMappings.put("10400", "10032");
        statusMappings.put("10500", "10020");
        statusMappings.put("10501", "10020");
        statusMappings.put("10600", "10020");
        statusMappings.put("10601", "10032");
        statusMappings.put("10602", "10042");
        statusMappings.put("10700", "10020");
        statusMappings.put("10800", "10020");
        statusMappings.put("10801", "10020");
        statusMappings.put("10802", "10020");
        statusMappings.put("10900", "10033");
        statusMappings.put("11000", "10020");
        statusMappings.put("11001", "10020");
        statusMappings.put("11002", "10032");
        statusMappings.put("11003", "10020");
        statusMappings.put("11004", "10020");
        statusMappings.put("11005", "10020");
        statusMappings.put("11006", "10020");
        statusMappings.put("11100", "10100");
        statusMappings.put("11200", "10020");
        statusMappings.put("3", "10032");
        statusMappings.put("4", "10032");
        statusMappings.put("5", "5");
        statusMappings.put("6", "6");

        cfMappings.put("11116", "10207");
        cfMappings.put("10937", "10206");
        cfMappings.put("11300", "10208");
        cfMappings.put("10961", "10214");
        cfMappings.put("10945", "10215");
        cfMappings.put("10204", "10119");
        cfMappings.put("10001", "10109");
        cfMappings.put("11402", "10103");
        cfMappings.put("11401", "10102");
        cfMappings.put("10226", "10150");
        cfMappings.put("10223", "10151");
        cfMappings.put("10911", "10148");
        cfMappings.put("10002", "10110");
        cfMappings.put("10225", "10149");
        cfMappings.put("12306", "10211");
        cfMappings.put("10000", "10108");
        cfMappings.put("10962", "10132");
        cfMappings.put("10953", "10192");
        cfMappings.put("10951", "10193");
        cfMappings.put("10921", "10159");
        cfMappings.put("11404", "10105");
        cfMappings.put("11406", "10106");
        cfMappings.put("10206", "10137");
        cfMappings.put("10205", "10136");
        sdMappings.put("10009", "a583ae69-b9a5-4399-9c7d-11a064ab8d97");
        sdMappings.put("10013", "b8950128-840e-4bfb-af74-819b21fb18cb");
        sdMappings.put("10010", "0c24cead-f5ff-4f1e-a74d-9f4af3e2bee8");
        sdMappings.put("10015", "88f27145-ea0f-48ea-965f-9d40ee20be60");
        sdMappings.put("10015", "1029473c-4a12-402d-a356-e950ea9826a3");
        sdMappings.put("10100", "953f4927-2585-46c8-a5ac-37648c52f591");
        sdMappings.put("10100", "3d3929f8-deb2-4aa6-812b-cad9f2316b40");
        sdMappings.put("10100", "f56b3d7b-81ae-442d-ab5e-994663d96d99");

        wfMappings.put("10002", "classic default workflow");
        wfMappings.put("10003", "classic default workflow");
        wfMappings.put("10009", "Default BRM Request Workflow");
        wfMappings.put("10010", "Default BRM Incident Workflow");
        wfMappings.put("10013", "User Creation & Authorization Workflow");
        wfMappings.put("10015", "HelpDesk Workflow");
        wfMappings.put("10016", "Generic Request Development Workflow");
        wfMappings.put("10017", "Generic Incident Development Workflow");
        wfMappings.put("10018", "SAP Request Development");
        wfMappings.put("10019", "SAP Incident Development");
        wfMappings.put("10020", "Default Effort Estimation Workflow");
        wfMappings.put("10100", "Feedback workflow");
    }

}
