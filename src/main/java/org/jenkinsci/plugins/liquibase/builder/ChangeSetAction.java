package org.jenkinsci.plugins.liquibase.builder;

import java.util.List;

import com.google.common.collect.Lists;

public class ChangeSetAction {
    private String id;
    private String author;
    private String comment;
    private String executionTime;
    private String result;


    private List<String> sqls;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<String> getSqls() {
        return sqls;
    }

    public void setSqls(List<String> sqls) {
        this.sqls = sqls;
    }

    public boolean hasSql() {
        return !sqls.isEmpty();
    }
    @Override
    public String toString() {
        return "ChangeSetAction{" +
                "id='" + id + '\'' +
                ", author='" + author + '\'' +
                ", comment='" + comment + '\'' +
                ", executionTime='" + executionTime + '\'' +
                ", result='" + result + '\'' +
                ", sqls=" + sqls +
                '}';
    }

    public String getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(String executionTime) {
        this.executionTime = executionTime;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public void addSql(String sql) {
        if (sqls == null) {
            sqls = Lists.newArrayList();
        }
        sqls.add(sql);
    }

}
