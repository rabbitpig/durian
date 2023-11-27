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
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.generator.api.GeneratedJavaFile;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.config.JavaModelGeneratorConfiguration;
import org.mybatis.generator.config.TableConfiguration;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 */
public class MysqlBasePlugin extends PluginAdapter {

    static String LOMBOK_EQUALS_AND_HASH_CODE = lombok.EqualsAndHashCode.class.getName();
    static Set<String> BASE_MODEL_FIELDS_SET = Sets.newHashSet();
    static Set<String> BASE_MODEL_METHODS_SET = Sets.newHashSet();

    static String FULLY_QUALIFIED_PAGE = com.cmcc.coc.ummp.common.common.bean.Page.class.getName();
    static String XMLFILE_POSTFIX = "Ext";
    static String JAVAFILE_POTFIX = "Ext";
    static String ANNOTATION_RESOURCE = "org.apache.ibatis.annotations.Mapper";
    static String XSL_BASE_PATH = "/generator/xsl";

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    /**
     * 获取主键类型
     * 
     * @param introspectedTable
     * @return
     */
    Id.Type getGeneratedKeyType(IntrospectedTable introspectedTable) {
        Id.Type type = Id.Type.MYSQL;
        if (introspectedTable.getGeneratedKey().getRuntimeSqlStatement().contains("IdWorker")) {
            type = Id.Type.ID_WORKER;
        }
        return type;
    }

    void recordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        if (enableLombok()) {
            topLevelClass.addImportedType(new FullyQualifiedJavaType(lombok.Data.class.getName()));
            topLevelClass.addImportedType(new FullyQualifiedJavaType(lombok.ToString.class.getName()));
            topLevelClass.addAnnotation("@Data");
            FullyQualifiedJavaType lombokEqualsAndHashCode = new FullyQualifiedJavaType(LOMBOK_EQUALS_AND_HASH_CODE);
            topLevelClass.addImportedType(lombokEqualsAndHashCode);
            topLevelClass.addAnnotation("@EqualsAndHashCode(callSuper = true)");
            topLevelClass.addAnnotation("@ToString(callSuper = true)");
        }

        // 根据配置，如果是idwork的主键，需要打上注解
        topLevelClass.getFields().stream().filter(field -> field.getName().equalsIgnoreCase(CloumnEnum.ID.getSql()))
            .forEach(field -> {
                if (getGeneratedKeyType(introspectedTable) == Id.Type.ID_WORKER) {
                    topLevelClass.addImportedType(new FullyQualifiedJavaType(Id.class.getName()));
                    field.addAnnotation("@Id(type = Id.Type.ID_WORKER)");
                }
            });

        Iterator methods = topLevelClass.getMethods().iterator();

        while (methods.hasNext()) {
            Method currentMethod = (Method) methods.next();
            if (enableLombok() || BASE_MODEL_METHODS_SET.contains(currentMethod.getName())) {
                methods.remove();
            }
        }
    }

    /**
     * * 是否使用lombok注解<br/> -Dmybatis.generator.myPlugins.enableLombok=true<br/>
     *
     * @return
     */
    Boolean enableLombok() {
        Boolean enableLombok = Boolean.valueOf(getProperties().getProperty("enableLombok",
                System.getProperty("mybatis.generator.myPlugins.enableLombok", "false")));
        return enableLombok;
    }

    /**
     * 是否生成manager<br/>
     * 默认为false
     *
     * @return
     */
    Boolean enableGenerateManager() {
        Boolean generateManager = Boolean.valueOf(getProperties().getProperty("generateManager",
                System.getProperty("mybatis.generator.myPlugins.mybatis.generator.myPlugins.manager", "false")));
        System.out.println("generateManager : " + generateManager);
        return generateManager;
    }

    /**
     * 是否生成controller<br/>
     * 默认为false
     *
     * @return
     */
    Boolean enableGenerateContropller() {
        Boolean generateContropller = Boolean.valueOf(getProperties().getProperty("generateController",
                System.getProperty("mybatis.generator.myPlugins.controller", "false")));
        System.out.println("generateContropller : " + generateContropller);
        return generateContropller;
    }

    /**
     * 获取实体类
     *
     * @param insertBatchElement
     * @return
     */
    String getTypeHandler(IntrospectedColumn insertBatchElement) {
        return insertBatchElement.getTypeHandler() != null ? ",typeHandler=" + insertBatchElement.getTypeHandler() : "";
    }

    /**
     * 获取类
     *
     * @param introspectedTable
     * @return
     */
    String getRecorType(IntrospectedTable introspectedTable) {
        return introspectedTable.getRules().generateRecordWithBLOBsClass() ? introspectedTable.getRecordWithBLOBsType()
                : introspectedTable.getBaseRecordType();
    }

    /**
     * 获取主键列名，兼容命名规则：表名_id
     *
     * @param introspectedTable
     * @return
     */
    String getIdColumnName(IntrospectedTable introspectedTable) {
        String standardIdColumnName = "id";
        String unstandardIdColumnName = introspectedTable.getFullyQualifiedTableNameAtRuntime() + standardIdColumnName;
        return introspectedTable.getAllColumns().stream().filter(v -> StringUtils.equals(v.getActualColumnName(), unstandardIdColumnName)).findFirst().isPresent() ? unstandardIdColumnName : standardIdColumnName;
    }

    List<GeneratedJavaFile> generateManagerJavaFiles(IntrospectedTable introspectedTable) {
        String model = getRecorType(introspectedTable);
        FullyQualifiedJavaType modelType = new FullyQualifiedJavaType(model);
        String managerInterface = introspectedTable.getBaseRecordType().replaceFirst("dao", "service").replaceFirst("model", "manager") + "Manager";
        String managetImpl = introspectedTable.getBaseRecordType().replaceFirst("dao", "service").replaceFirst("model", "manager.impl") + "ManagerImpl";
        FullyQualifiedJavaType managerInterfaceType = new FullyQualifiedJavaType(managerInterface);
        Interface interfaze = new Interface(managerInterfaceType);
        interfaze.setVisibility(JavaVisibility.PUBLIC);
        FullyQualifiedJavaType iBaseManager = new FullyQualifiedJavaType(com.cmcc.coc.ummp.common.common.baseclass.IBaseManager.class.getName());
        iBaseManager.addTypeArgument(modelType);
        interfaze.addImportedType(iBaseManager);
        interfaze.addSuperInterface(iBaseManager);
        GeneratedJavaFile interfazeJavaFile = new GeneratedJavaFile(interfaze,
                this.context.getJavaModelGeneratorConfiguration().getTargetProject(),
                this.context.getProperty("javaFileEncoding"), this.context.getJavaFormatter());
        TopLevelClass topLevelClass = new TopLevelClass(new FullyQualifiedJavaType(managetImpl));
        topLevelClass.setVisibility(JavaVisibility.PUBLIC);
        topLevelClass.addAnnotation("@Service");
        topLevelClass.addImportedType(new FullyQualifiedJavaType("org.springframework.stereotype.Service"));
        FullyQualifiedJavaType baseManager = new FullyQualifiedJavaType(com.cmcc.coc.ummp.common.common.baseclass.BaseManager.class.getName());
        baseManager.addTypeArgument(modelType);
        topLevelClass.addImportedType(baseManager);
        topLevelClass.setSuperClass(baseManager);
        topLevelClass.addImportedType(managerInterfaceType);
        topLevelClass.addSuperInterface(managerInterfaceType);
        FullyQualifiedJavaType mapperType = new FullyQualifiedJavaType(
                introspectedTable.getMyBatis3JavaMapperType() + JAVAFILE_POTFIX);
        String mapperName = mapperType.getShortName();
        mapperName = mapperName.substring(0, 1).toLowerCase() + mapperName.substring(1);
        Field mapper = new Field(mapperName, mapperType);
        topLevelClass.addImportedType(mapperType);
        topLevelClass.addImportedType(
                new FullyQualifiedJavaType("org.springframework.beans.factory.annotation.Autowired"));
        mapper.addAnnotation("@Autowired");
        mapper.setVisibility(JavaVisibility.PRIVATE);
        topLevelClass.addField(mapper);
        Method getBaseMapper = new Method("getBaseMapper");
        getBaseMapper.setVisibility(JavaVisibility.PROTECTED);
        getBaseMapper.setReturnType(new FullyQualifiedJavaType("java.lang.Object"));
        getBaseMapper.addBodyLine("return " + mapperName + ";");
        getBaseMapper.addAnnotation("@Override");
        topLevelClass.addMethod(getBaseMapper);
        Method newExample = new Method("newExample");
        newExample.setVisibility(JavaVisibility.PROTECTED);
        newExample.setReturnType(new FullyQualifiedJavaType("java.lang.Object"));
        FullyQualifiedJavaType exampleType = new FullyQualifiedJavaType(introspectedTable.getExampleType());
        String exampleClassName = exampleType.getShortName();
        topLevelClass.addImportedType(exampleType);
        newExample.addBodyLine("return new " + exampleClassName + "();");
        newExample.addAnnotation("@Override");
        topLevelClass.addMethod(newExample);
        GeneratedJavaFile topLevelClassJavaFile = new GeneratedJavaFile(topLevelClass,
                this.context.getJavaModelGeneratorConfiguration().getTargetProject(),
                this.context.getProperty("javaFileEncoding"), this.context.getJavaFormatter());
        ArrayList generatedJavaFiles = new ArrayList();
        generatedJavaFiles.add(interfazeJavaFile);
        generatedJavaFiles.add(topLevelClassJavaFile);
        return generatedJavaFiles;
    }

    void generateController(IntrospectedTable introspectedTable) {
        try {
            org.w3c.dom.Document ex = this.generateDocument(introspectedTable);
            Map paramMap = this.generateParameters(introspectedTable);
            String modelName = (String) paramMap.get("modelName");
            String controllerPath = ((String) paramMap.get("controllerPackage")).replace(".", "/");
            String controllerXslIn = XSL_BASE_PATH + "/Controller.xsl";
            String controllerJavaOut = String.format("src/main/java/%s/%sController.java",
                    new Object[]{controllerPath, modelName});
            this.generateCodeFile(ex, controllerXslIn, controllerJavaOut, paramMap);
        } catch (Exception var11) {
            var11.printStackTrace();
        }

    }

    private Map<String, String> generateParameters(IntrospectedTable introspectedTable) {
        HashMap parameters = new HashMap();
        String tableName = introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime();
        String modelName = (String) this.generateTableJavaMap().get(tableName);
        JavaModelGeneratorConfiguration modelConfig = this.context.getJavaModelGeneratorConfiguration();
        String modelPackage = modelConfig.getTargetPackage();
        String managerPackage = modelPackage.replaceFirst("dao", "service").replaceFirst("model", "manager");
        String controllerPackage = modelPackage.replaceFirst("dao", "web").replaceFirst("model", "controller");
        parameters.put("modelName", modelName);
        parameters.put("modelPackage", modelPackage);
        parameters.put("managerPackage", managerPackage);
        parameters.put("controllerPackage", controllerPackage);
        return parameters;
    }

    private Map<String, String> generateTableJavaMap() {
        HashMap map = new HashMap();
        List tableConfigurations = this.context.getTableConfigurations();
        Iterator var3 = tableConfigurations.iterator();

        while (var3.hasNext()) {
            TableConfiguration tableConfiguration = (TableConfiguration) var3.next();
            String domainName = tableConfiguration.getDomainObjectName();
            String tableName = tableConfiguration.getTableName();
            map.put(tableName, domainName);
        }

        return map;
    }

    private org.w3c.dom.Document generateDocument(IntrospectedTable introspectedTable)
            throws ParserConfigurationException {
        org.w3c.dom.Document document = null;
        String tableName = introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime();
        String modelName = (String) this.generateTableJavaMap().get(tableName);
        ArrayList columnList = new ArrayList();
        List columns = introspectedTable.getAllColumns();
        Iterator factory = columns.iterator();

        while (factory.hasNext()) {
            IntrospectedColumn builder = (IntrospectedColumn) factory.next();
            ColumnData models = new ColumnData();
            models.setDbName(builder.getActualColumnName());
            models.setDbType(builder.getJdbcTypeName());
            models.setJavaName(builder.getJavaProperty());
            models.setJavaType((String) ColumnData.TYPE_MAPPINGS.get(builder.getJdbcTypeName().toLowerCase()));
            columnList.add(models);
        }

        DocumentBuilderFactory factory1 = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder1 = factory1.newDocumentBuilder();
        document = builder1.newDocument();
        org.w3c.dom.Element models1 = document.createElement("models");
        document.appendChild(models1);
        org.w3c.dom.Element model = document.createElement("model");
        models1.appendChild(model);
        model.setAttribute("name", modelName);
        String fname = modelName.substring(0, 1).toLowerCase() + modelName.substring(1, modelName.length());
        model.setAttribute("fname", fname);
        model.setAttribute("cname", tableName);
        org.w3c.dom.Element properties = document.createElement("properties");
        model.appendChild(properties);
        Iterator var12 = columnList.iterator();

        while (var12.hasNext()) {
            ColumnData column = (ColumnData) var12.next();
            if (!"id".equalsIgnoreCase(column.getJavaName())) {
                org.w3c.dom.Element property = document.createElement("property");
                properties.appendChild(property);
                property.setAttribute("name", column.getJavaName());
                property.setAttribute("cname", column.getDbName());
                property.setAttribute("type", column.getJavaType());
            }
        }

        return document;
    }

    private void generateCodeFile(org.w3c.dom.Document document, String xsltFileName, String outputFileName,
                                  Map<String, String> parameters) throws IOException,
            TransformerException {
        StreamSource styleSource = new StreamSource(this.getClass().getResourceAsStream(xsltFileName));
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer(styleSource);
        if (!(new File(outputFileName)).exists()) {
            this.generateTransform(document, parameters, outputFileName, transformer);
        }

    }

    private void generateTransform(org.w3c.dom.Document document, Map<String, String> parameters, String outputFileName,
                                   Transformer transformer)
            throws TransformerFactoryConfigurationError, FileNotFoundException, TransformerException {
        if (parameters != null) {
            Iterator out = parameters.entrySet().iterator();

            while (out.hasNext()) {
                Map.Entry outputFile = (Map.Entry) out.next();
                transformer.setParameter((String) outputFile.getKey(), outputFile.getValue());
            }
        }

        File out1 = new File(outputFileName);
        if (!out1.getParentFile().exists()) {
            boolean success = out1.getParentFile().mkdirs();
        }

        File outputFile1 = new File(outputFileName);
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(new FileOutputStream(outputFile1));
        transformer.transform(source, result);
    }
}
