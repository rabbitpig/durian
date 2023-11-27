/*
 * Copyright 2013 rabbitpig <admin@rabbitpig.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cmcc.coc.ummp.common.common.mybatis;

import com.cmcc.coc.ummp.common.common.baseclass.Id;
import org.mybatis.generator.api.*;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.api.dom.xml.*;
import org.mybatis.generator.config.GeneratedKey;
import org.mybatis.generator.internal.util.messages.Messages;

import java.io.File;
import java.util.*;

/**
 * ClassName: MySQLPaginationPlugin <br/>
 * Function: Mybatis Genetator Mysql分页插件. <br/>
 * 参考文档：http://mybatis.github.io/generator/reference/pluggingIn.html<br/>
 * Reason: Mybatis Genetator Mysql分页插件. <br/>
 */
@SuppressWarnings("all")
public class MySQLLogicalDeletePlugin extends MysqlBasePlugin {

    public MySQLLogicalDeletePlugin() {
        super();
    }

    public static void main(String[] args) {
        String config = MySQLLogicalDeletePlugin.class.getClassLoader().getResource("config.xml").getFile();
        String[] arg = {"-configfile", config};
        ShellRunner.main(arg);
    }

    @Override
    public boolean modelExampleClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        topLevelClass.addAnnotation("@SuppressWarnings(\"all\")");
        ((Field) topLevelClass.getFields().get(0)).setInitializationString("\"id asc\"");
        this.addPage(topLevelClass, introspectedTable, "page");
        return super.modelExampleClassGenerated(topLevelClass, introspectedTable);
    }

    private void addPage(TopLevelClass topLevelClass, IntrospectedTable introspectedTable, String name) {
        FullyQualifiedJavaType page = new FullyQualifiedJavaType(FULLY_QUALIFIED_PAGE);
        topLevelClass.addImportedType(page);
        CommentGenerator commentGenerator = this.context.getCommentGenerator();
        Field field = new Field();
        field.setVisibility(JavaVisibility.PROTECTED);
        field.setType(page);
        field.setName(name);
        commentGenerator.addFieldComment(field, introspectedTable);
        topLevelClass.addField(field);
        char c = name.charAt(0);
        String camel = Character.toUpperCase(c) + name.substring(1);
        Method method = new Method();
        method.setVisibility(JavaVisibility.PUBLIC);
        method.setName("set" + camel);
        method.addParameter(new Parameter(page, name));
        method.addBodyLine("this." + name + "=" + name + ";");
        commentGenerator.addGeneralMethodComment(method, introspectedTable);
        topLevelClass.addMethod(method);
        method = new Method();
        method.setVisibility(JavaVisibility.PUBLIC);
        method.setReturnType(page);
        method.setName("get" + camel);
        method.addBodyLine("return " + name + ";");
        commentGenerator.addGeneralMethodComment(method, introspectedTable);
        topLevelClass.addMethod(method);
    }

    /**
     * 逻辑删除时，需要将过滤条件与isDeleted='n'用and连接
     *
     * @param element
     * @param introspectedTable
     * @return
     */
    @Override
    public boolean sqlMapExampleWhereClauseElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        XmlElement trimElement = new XmlElement("trim");
        trimElement.addAttribute(new Attribute("prefix", "and (("));
        trimElement.addAttribute(new Attribute("prefixOverrides", "("));
        trimElement.addAttribute(new Attribute("suffix", "))"));
        trimElement.addAttribute(new Attribute("suffixOverrides", ")"));
        XmlElement whereElement = (XmlElement) element.getElements().get(5);
        trimElement.addElement((Element) whereElement.getElements().get(0));
        whereElement.getElements().remove(0);
        whereElement.addElement(trimElement);

        TextElement isDeletedElement = new TextElement("and " + CloumnEnum.IS_DELETED.getSql() + " = 0");
        whereElement.addElement(isDeletedElement);

        return super.sqlMapExampleWhereClauseElementGenerated(element, introspectedTable);
    }

    private void addBatchInsertMethod(Interface interfaze, IntrospectedTable introspectedTable) {
        TreeSet importedTypes = new TreeSet();
        importedTypes.add(FullyQualifiedJavaType.getNewListInstance());
        importedTypes.add(new FullyQualifiedJavaType(getRecorType(introspectedTable)));
        Method ibsmethod = new Method();
        ibsmethod.setVisibility(JavaVisibility.PUBLIC);
        FullyQualifiedJavaType ibsreturnType = FullyQualifiedJavaType.getIntInstance();
        ibsmethod.setReturnType(ibsreturnType);
        ibsmethod.setName("insertBatch");
        FullyQualifiedJavaType paramType = FullyQualifiedJavaType.getNewListInstance();
        FullyQualifiedJavaType paramListType;
        if (introspectedTable.getRules().generateBaseRecordClass()) {
            paramListType = new FullyQualifiedJavaType(getRecorType(introspectedTable));
        } else {
            if (!introspectedTable.getRules().generatePrimaryKeyClass()) {
                throw new RuntimeException(Messages.getString("RuntimeError.12"));
            }

            paramListType = new FullyQualifiedJavaType(introspectedTable.getPrimaryKeyType());
        }

        paramType.addTypeArgument(paramListType);
        ibsmethod.addParameter(new Parameter(paramType, "records"));
        interfaze.addImportedTypes(importedTypes);
        interfaze.addMethod(ibsmethod);
    }

    public void addBatchInsertXml(XmlElement element, IntrospectedTable introspectedTable) {
        List columns = introspectedTable.getAllColumns();
        GeneratedKey generatedKey = introspectedTable.getTableConfiguration().getGeneratedKey();
        Id.Type idType = getGeneratedKeyType(introspectedTable);
        String incrementField = generatedKey.getColumn();
        if (incrementField != null) {
            incrementField = incrementField.toUpperCase();
        }

        StringBuilder dbcolumnsName = new StringBuilder();
        StringBuilder javaPropertyAndDbType = new StringBuilder();
        Iterator tableName = columns.iterator();

        while (tableName.hasNext()) {
            IntrospectedColumn insertBatchElement = (IntrospectedColumn) tableName.next();
            String trim1Element = insertBatchElement.getActualColumnName();
            if (idType == Id.Type.ID_WORKER || !trim1Element.toUpperCase().equals(incrementField)) {
                dbcolumnsName.append("`" + trim1Element + "`,");
                javaPropertyAndDbType.append(
                        "#{item." + insertBatchElement.getJavaProperty() + ",jdbcType=" + insertBatchElement
                                .getJdbcTypeName() + getTypeHandler(insertBatchElement) + "},");
            }
        }

        String tableName1 = introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime();
        XmlElement insertBatchElement1 = new XmlElement("insert");
        this.context.getCommentGenerator().addComment(insertBatchElement1);
        insertBatchElement1.addAttribute(new Attribute("id", "insertBatch"));
        insertBatchElement1.addAttribute(new Attribute("parameterType", getRecorType(introspectedTable)));
        insertBatchElement1.addAttribute(new Attribute("useGeneratedKeys", "true"));
        insertBatchElement1.addAttribute(new Attribute("keyProperty", "id"));

        insertBatchElement1.addElement(new TextElement("insert into " + tableName1));
        XmlElement trim1Element1 = new XmlElement("trim");
        trim1Element1.addAttribute(new Attribute("prefix", "("));
        trim1Element1.addAttribute(new Attribute("suffix", ")"));
        trim1Element1.addAttribute(new Attribute("suffixOverrides", ","));
        trim1Element1.addElement(new TextElement(dbcolumnsName.toString()));
        insertBatchElement1.addElement(trim1Element1);
        insertBatchElement1.addElement(new TextElement("values"));
        XmlElement foreachElement = new XmlElement("foreach");
        foreachElement.addAttribute(new Attribute("collection", "list"));
        foreachElement.addAttribute(new Attribute("index", "index"));
        foreachElement.addAttribute(new Attribute("item", "item"));
        foreachElement.addAttribute(new Attribute("separator", ","));
        foreachElement.addElement(new TextElement("("));
        XmlElement trim2Element = new XmlElement("trim");
        trim2Element.addAttribute(new Attribute("suffixOverrides", ","));
        trim2Element.addElement(new TextElement(javaPropertyAndDbType.toString()));
        foreachElement.addElement(trim2Element);
        foreachElement.addElement(new TextElement(")"));
        insertBatchElement1.addElement(foreachElement);
        element.addElement(insertBatchElement1);
    }

    private void addBatchUpdateMethod(Interface interfaze, IntrospectedTable introspectedTable) {
        TreeSet importedTypes = new TreeSet();
        importedTypes.add(FullyQualifiedJavaType.getNewListInstance());
        importedTypes.add(new FullyQualifiedJavaType(getRecorType(introspectedTable)));
        Method ibsmethod = new Method();
        ibsmethod.setVisibility(JavaVisibility.PUBLIC);
        FullyQualifiedJavaType ibsreturnType = FullyQualifiedJavaType.getIntInstance();
        ibsmethod.setReturnType(ibsreturnType);
        ibsmethod.setName("updateBatch");
        FullyQualifiedJavaType paramType = FullyQualifiedJavaType.getNewListInstance();
        FullyQualifiedJavaType paramListType;
        if (introspectedTable.getRules().generateBaseRecordClass()) {
            paramListType = new FullyQualifiedJavaType(getRecorType(introspectedTable));
        } else {
            if (!introspectedTable.getRules().generatePrimaryKeyClass()) {
                throw new RuntimeException(Messages.getString("RuntimeError.12"));
            }

            paramListType = new FullyQualifiedJavaType(introspectedTable.getPrimaryKeyType());
        }

        paramType.addTypeArgument(paramListType);
        ibsmethod.addParameter(new Parameter(paramType, "records"));
        interfaze.addImportedTypes(importedTypes);
        interfaze.addMethod(ibsmethod);
    }

    public void addBatchUpdateXml(XmlElement element, IntrospectedTable introspectedTable) {
        List columns = introspectedTable.getAllColumns();
        String incrementField = introspectedTable.getTableConfiguration().getGeneratedKey().getColumn();
        XmlElement insertBatchElement = new XmlElement("update");
        this.context.getCommentGenerator().addComment(insertBatchElement);
        insertBatchElement.addAttribute(new Attribute("id", "updateBatch"));
        insertBatchElement.addAttribute(new Attribute("parameterType", getRecorType(introspectedTable)));
        XmlElement foreachElement = new XmlElement("foreach");
        foreachElement.addAttribute(new Attribute("collection", "list"));
        foreachElement.addAttribute(new Attribute("index", "index"));
        foreachElement.addAttribute(new Attribute("item", "item"));
        foreachElement.addAttribute(new Attribute("separator", ";"));
        foreachElement.addElement(
                new TextElement("update " + introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime()));
        XmlElement setElement = new XmlElement("set");
        Iterator var8 = columns.iterator();

        while (true) {
            IntrospectedColumn introspectedColumn;
            String columnName;
            do {
                if (!var8.hasNext()) {
                    foreachElement.addElement(setElement);
                    foreachElement.addElement(
                            new TextElement("where " + incrementField + " = #{item." + incrementField + "}"));
                    customWhereSqlForPrimaryKey(foreachElement);
                    insertBatchElement.addElement(foreachElement);
                    element.addElement(insertBatchElement);
                    return;
                }

                introspectedColumn = (IntrospectedColumn) var8.next();
                columnName = introspectedColumn.getActualColumnName();
            } while (incrementField != null && incrementField.toUpperCase().equals(columnName.toUpperCase()));

            setElement.addElement(new TextElement(
                    "`" + columnName + "` = #{item." + introspectedColumn.getJavaProperty() + ",jdbcType="
                            + introspectedColumn.getJdbcTypeName() + getTypeHandler(introspectedColumn) + "},"));
        }
    }

    @Override
    public boolean sqlMapSelectByPrimaryKeyElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        customWhereSqlForPrimaryKey(element);
        return super.sqlMapSelectByPrimaryKeyElementGenerated(element, introspectedTable);
    }

    @Override
    public boolean sqlMapSelectByExampleWithBLOBsElementGenerated(XmlElement element,
                                                                  IntrospectedTable introspectedTable) {
        XmlElement pageStart = new XmlElement("include");
        pageStart.addAttribute(new Attribute("refid", "MysqlDialectPrefix"));
        element.getElements().add(10, pageStart);

        XmlElement isNotNullElement = new XmlElement("include");
        isNotNullElement.addAttribute(new Attribute("refid", "MysqlDialectSuffix"));
        element.getElements().add(isNotNullElement);
        return super.sqlMapSelectByExampleWithBLOBsElementGenerated(element, introspectedTable);
    }

    @Override
    public boolean sqlMapSelectByExampleWithoutBLOBsElementGenerated(XmlElement element,
                                                                     IntrospectedTable introspectedTable) {
        XmlElement pageStart = new XmlElement("include");
        pageStart.addAttribute(new Attribute("refid", "MysqlDialectPrefix"));
        element.getElements().add(8, pageStart);

        XmlElement isNotNullElement = new XmlElement("include");
        isNotNullElement.addAttribute(new Attribute("refid", "MysqlDialectSuffix"));
        element.getElements().add(isNotNullElement);
        return super.sqlMapSelectByExampleWithoutBLOBsElementGenerated(element, introspectedTable);
    }

    @Override
    public boolean sqlMapCountByExampleElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return super.sqlMapCountByExampleElementGenerated(element, introspectedTable);
    }

    @Override
    public boolean sqlMapUpdateByPrimaryKeyWithoutBLOBsElementGenerated(XmlElement element,
                                                                        IntrospectedTable introspectedTable) {
        customWhereSqlForPrimaryKey(element);
        return super.sqlMapUpdateByPrimaryKeyWithoutBLOBsElementGenerated(element, introspectedTable);
    }

    @Override
    public boolean sqlMapUpdateByPrimaryKeyWithBLOBsElementGenerated(XmlElement element,
                                                                     IntrospectedTable introspectedTable) {
        customWhereSqlForPrimaryKey(element);
        return super.sqlMapUpdateByPrimaryKeyWithBLOBsElementGenerated(element, introspectedTable);
    }

    @Override
    public boolean sqlMapUpdateByExampleWithBLOBsElementGenerated(XmlElement element,
                                                                  IntrospectedTable introspectedTable) {
        return super.sqlMapUpdateByExampleWithBLOBsElementGenerated(element, introspectedTable);
    }

    @Override
    public boolean sqlMapUpdateByExampleWithoutBLOBsElementGenerated(XmlElement element,
                                                                     IntrospectedTable introspectedTable) {
        return super.sqlMapUpdateByExampleWithoutBLOBsElementGenerated(element, introspectedTable);
    }

    @Override
    public boolean sqlMapUpdateByPrimaryKeySelectiveElementGenerated(XmlElement element,
                                                                     IntrospectedTable introspectedTable) {
        customWhereSqlForPrimaryKey(element);
        return super.sqlMapUpdateByPrimaryKeySelectiveElementGenerated(element, introspectedTable);
    }

    @Override
    public boolean sqlMapUpdateByExampleSelectiveElementGenerated(XmlElement element,
                                                                  IntrospectedTable introspectedTable) {
        return super.sqlMapUpdateByExampleSelectiveElementGenerated(element, introspectedTable);
    }

    @Override
    public boolean sqlMapDeleteByPrimaryKeyElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        element.setName("update");
        int replaceInd = -1;
        for (int i = 0; i < element.getAttributes().size(); i++) {
            Attribute attr = element.getAttributes().get(i);
            if ("parameterType".equals(attr.getName())) {
                replaceInd = i;
                break;
            }
        }
        if (replaceInd >= 0) {
            element.getAttributes().remove(replaceInd);
            element.getAttributes().add(replaceInd,
                    new Attribute("parameterType", "map"));
        }
        List<Element> removeElement = new ArrayList<>();
        for (int i = 5; i < element.getElements().size(); i++) {
            removeElement.add(element.getElements().get(i));

        }
        element.getElements().removeAll(removeElement);

        element.getElements().add(new TextElement(
                "update " + introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime() + " set "
                        + CloumnEnum.IS_DELETED.getSql() + " = " + getIdColumnName(introspectedTable) + ","
                        + CloumnEnum.UPDATE_AT.getSql() + " = #{record." + CloumnEnum.UPDATE_AT.getClazz() + "},"
                        + CloumnEnum.UPDATE_BY_NAME.getSql() + " = #{record." + CloumnEnum.UPDATE_BY_NAME.getClazz() + "},"
                        + CloumnEnum.UPDATE_BY.getSql() + " = #{record." + CloumnEnum.UPDATE_BY.getClazz() + "}"
                        + " where id = #{id,jdbcType=BIGINT}"));

        customWhereSqlForPrimaryKey(element);

        return super.sqlMapDeleteByPrimaryKeyElementGenerated(element, introspectedTable);
    }

    /**
     * 逻辑删除时，需要替换删除接口，新增record参数传入修改人修改时间
     *
     * @param method
     * @param interfaze
     * @param introspectedTable
     * @return
     */
    @Override
    public boolean clientDeleteByPrimaryKeyMethodGenerated(Method method, Interface interfaze,
                                                           IntrospectedTable introspectedTable) {
        interfaze.addImportedType(new FullyQualifiedJavaType("org.apache.ibatis.annotations.Param"));

        method.getParameters().get(0).addAnnotation("@Param(\"id\")");

        Parameter model = new Parameter(new FullyQualifiedJavaType(getRecorType(introspectedTable)), "record");
        model.addAnnotation("@Param(\"record\")");
        method.addParameter(0, model);

        return super.clientDeleteByPrimaryKeyMethodGenerated(method, interfaze, introspectedTable);
    }

    @Override
    public boolean sqlMapDeleteByExampleElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        element.setName("update");
        int replaceInd = -1;
        for (int i = 0; i < element.getAttributes().size(); i++) {
            Attribute attr = element.getAttributes().get(i);
            if ("parameterType".equals(attr.getName())) {
                replaceInd = i;
                break;
            }
        }
        if (replaceInd >= 0) {
            element.getAttributes().remove(replaceInd);
            element.getAttributes().add(replaceInd,
                    new Attribute("parameterType", "map"));
        }
        List<Element> removeElement = new ArrayList<>();
        for (int i = 5; i < element.getElements().size(); i++) {
            removeElement.add(element.getElements().get(i));

        }
        element.getElements().removeAll(removeElement);
        element.getElements().add(new TextElement(
                "update " + introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime() + " set "
                        + CloumnEnum.IS_DELETED.getSql() + " = " + getIdColumnName(introspectedTable) + ","
                        + CloumnEnum.UPDATE_AT.getSql() + " = #{record." + CloumnEnum.UPDATE_AT.getClazz() + "},"
                        + CloumnEnum.UPDATE_BY_NAME.getSql() + " = #{record." + CloumnEnum.UPDATE_BY_NAME.getClazz() + "},"
                        + CloumnEnum.UPDATE_BY.getSql() + " = #{record." + CloumnEnum.UPDATE_BY.getClazz() + "}"));

        XmlElement includeElement = new XmlElement("include");
        includeElement.addAttribute(new Attribute("refid", "Update_By_Example_Where_Clause"));
        XmlElement ifElement = new XmlElement("if");
        ifElement.addAttribute(new Attribute("test", "_parameter != null"));
        ifElement.addElement(includeElement);
        element.addElement(ifElement);

        return super.sqlMapDeleteByExampleElementGenerated(element, introspectedTable);
    }

    /**
     * 逻辑删除时，需要替换删除接口，新增record参数传入修改人修改时间
     *
     * @param method
     * @param interfaze
     * @param introspectedTable
     * @return
     */
    @Override
    public boolean clientDeleteByExampleMethodGenerated(Method method, Interface interfaze,
                                                        IntrospectedTable introspectedTable) {
        interfaze.addImportedType(new FullyQualifiedJavaType("org.apache.ibatis.annotations.Param"));

        method.getParameters().get(0).addAnnotation("@Param(\"example\")");

        Parameter model = new Parameter(new FullyQualifiedJavaType(getRecorType(introspectedTable)), "record");
        model.addAnnotation("@Param(\"record\")");
        method.addParameter(0, model);

        return super.clientDeleteByExampleMethodGenerated(method, interfaze, introspectedTable);
    }

    private void addPageSql(XmlElement element, IntrospectedTable introspectedTable) {
        String tableName = introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime();
        XmlElement paginationPrefixElement = new XmlElement("sql");
        this.context.getCommentGenerator().addComment(paginationPrefixElement);
        paginationPrefixElement.addAttribute(new Attribute("id", "MysqlDialectPrefix"));
        XmlElement pageStart = new XmlElement("if");
        pageStart.addAttribute(new Attribute("test", "page != null"));
        pageStart.addElement(new TextElement("from " + tableName + " , ( select id as temp_page_table_id "));
        paginationPrefixElement.addElement(pageStart);
        element.addElement(paginationPrefixElement);
        XmlElement orderByClause = new XmlElement("if");
        orderByClause.addAttribute(new Attribute("test", "orderByClause != null"));
        orderByClause.addElement(new TextElement("order by ${orderByClause}"));
        XmlElement paginationSuffixElement = new XmlElement("sql");
        this.context.getCommentGenerator().addComment(paginationSuffixElement);
        paginationSuffixElement.addAttribute(new Attribute("id", "MysqlDialectSuffix"));
        XmlElement pageEnd = new XmlElement("if");
        pageEnd.addAttribute(new Attribute("test", "page != null"));
        pageEnd.addElement(new TextElement(
                "<![CDATA[ limit #{page.offset}, #{page.limit} ) as temp_page_table ]]> where " + tableName
                        + ".id=temp_page_table.temp_page_table_id"));
        pageEnd.addElement(orderByClause);
        paginationSuffixElement.addElement(pageEnd);
        element.addElement(paginationSuffixElement);
    }

    @Override
    public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
        XmlElement parentElement = document.getRootElement();
        this.updateDocumentNameSpace(introspectedTable, parentElement);
        this.addPageSql(parentElement, introspectedTable);
        this.addBatchInsertXml(parentElement, introspectedTable);
        this.addBatchUpdateXml(parentElement, introspectedTable);
        return super.sqlMapDocumentGenerated(document, introspectedTable);
    }

    @Override
    public boolean clientGenerated(Interface interfaze, TopLevelClass topLevelClass,
                                   IntrospectedTable introspectedTable) {
        this.addBatchInsertMethod(interfaze, introspectedTable);
        this.addBatchUpdateMethod(interfaze, introspectedTable);
        return super.clientGenerated(interfaze, topLevelClass, introspectedTable);
    }

    private void updateDocumentNameSpace(IntrospectedTable introspectedTable, XmlElement parentElement) {
        Attribute namespaceAttribute = null;
        Iterator var4 = parentElement.getAttributes().iterator();

        while (var4.hasNext()) {
            Attribute attribute = (Attribute) var4.next();
            if ("namespace".equals(attribute.getName())) {
                namespaceAttribute = attribute;
            }
        }

        parentElement.getAttributes().remove(namespaceAttribute);
        parentElement.getAttributes().add(
                new Attribute("namespace", introspectedTable.getMyBatis3JavaMapperType() + JAVAFILE_POTFIX));
    }

    @Override
    public List<GeneratedXmlFile> contextGenerateAdditionalXmlFiles(IntrospectedTable introspectedTable) {
        String[] splitFile = introspectedTable.getMyBatis3XmlMapperFileName().split("\\.");
        String fileNameExt = null;
        if (splitFile[0] != null) {
            fileNameExt = splitFile[0] + XMLFILE_POSTFIX + ".xml";
        }

        if (this.isExistExtFile(this.context.getSqlMapGeneratorConfiguration().getTargetProject(),
                introspectedTable.getMyBatis3XmlMapperPackage(), fileNameExt)) {
            return super.contextGenerateAdditionalXmlFiles(introspectedTable);
        } else {
            Document document = new Document("-//mybatis.org//DTD Mapper 3.0//EN",
                    "http://mybatis.org/dtd/mybatis-3-mapper.dtd");
            XmlElement root = new XmlElement("mapper");
            document.setRootElement(root);
            String namespace = introspectedTable.getMyBatis3SqlMapNamespace() + XMLFILE_POSTFIX;
            root.addAttribute(new Attribute("namespace", namespace));

            GeneratedXmlFile gxf = new GeneratedXmlFile(document, fileNameExt,
                    introspectedTable.getMyBatis3XmlMapperPackage(),
                    this.context.getSqlMapGeneratorConfiguration().getTargetProject(), false,
                    this.context.getXmlFormatter());
            ArrayList answer = new ArrayList(1);
            answer.add(gxf);
            return answer;
        }
    }

    @Override
    public List<GeneratedJavaFile> contextGenerateAdditionalJavaFiles(IntrospectedTable introspectedTable) {
        FullyQualifiedJavaType type = new FullyQualifiedJavaType(
                introspectedTable.getMyBatis3JavaMapperType() + JAVAFILE_POTFIX);
        Interface interfaze = new Interface(type);
        interfaze.setVisibility(JavaVisibility.PUBLIC);
        this.context.getCommentGenerator().addJavaFileComment(interfaze);
        FullyQualifiedJavaType baseInterfaze = new FullyQualifiedJavaType(
                introspectedTable.getMyBatis3JavaMapperType());
        interfaze.addSuperInterface(baseInterfaze);
        FullyQualifiedJavaType annotation = new FullyQualifiedJavaType(ANNOTATION_RESOURCE);
        interfaze.addAnnotation("@Mapper");
        interfaze.addImportedType(annotation);
        GeneratedJavaFile generatedJavaFile = new GeneratedJavaFile(interfaze,
                this.context.getJavaModelGeneratorConfiguration().getTargetProject(),
                this.context.getProperty("javaFileEncoding"), this.context.getJavaFormatter());
        ArrayList generatedJavaFiles = new ArrayList(1);
        generatedJavaFile.getFileName();
        generatedJavaFiles.add(generatedJavaFile);
        if (enableGenerateManager()) {
            generatedJavaFiles.addAll(this.generateManagerJavaFiles(introspectedTable));
        }
        Iterator fileIterator = generatedJavaFiles.iterator();

        while (fileIterator.hasNext()) {
            GeneratedJavaFile temp = (GeneratedJavaFile) fileIterator.next();
            if (this.isExistExtFile(temp.getTargetProject(), temp.getTargetPackage(), temp.getFileName())) {
                fileIterator.remove();
            }
        }
        if (enableGenerateContropller()) {
            this.generateController(introspectedTable);
        }
        return generatedJavaFiles;
    }

    private boolean isExistExtFile(String targetProject, String targetPackage, String fileName) {
        File project = new File(targetProject);
        StringBuilder sb = new StringBuilder();
        StringTokenizer st = new StringTokenizer(targetPackage, ".");

        while (st.hasMoreTokens()) {
            sb.append(st.nextToken());
            sb.append(File.separatorChar);
        }

        File directory = new File(project, sb.toString());
        if (!directory.isDirectory()) {
            boolean testFile = directory.mkdirs();
            if (!testFile) {
                return true;
            }
        }

        File testFile1 = new File(directory, fileName);
        return testFile1.exists();
    }

    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        //给DO增加默认接口
        FullyQualifiedJavaType baseDoType = new FullyQualifiedJavaType(com.cmcc.coc.ummp.common.common.baseclass.BaseModel.class.getName());
        topLevelClass.addImportedType(baseDoType);
        topLevelClass.setSuperClass(baseDoType);

        Iterator fileds = topLevelClass.getFields().iterator();

        while (fileds.hasNext()) {
            Field methods = (Field) fileds.next();
            if (BASE_MODEL_FIELDS_SET.contains(methods.getName())) {
                fileds.remove();
            }
        }

        recordClassGenerated(topLevelClass, introspectedTable);

        return super.modelBaseRecordClassGenerated(topLevelClass, introspectedTable);
    }

    @Override
    public boolean modelRecordWithBLOBsClassGenerated(TopLevelClass topLevelClass,
                                                      IntrospectedTable introspectedTable) {
        recordClassGenerated(topLevelClass, introspectedTable);
        return super.modelRecordWithBLOBsClassGenerated(topLevelClass, introspectedTable);
    }

    private void customWhereSqlForPrimaryKey(XmlElement element) {
        TextElement text = new TextElement("and " + CloumnEnum.IS_DELETED.getSql() + " = 0");
        element.addElement(text);
    }
}

