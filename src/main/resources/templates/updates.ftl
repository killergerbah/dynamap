<#--
    Copyright 2017 N3TWORK INC

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
-->

<#include "common.ftl">

package ${package};

<#list imports as import>
import ${import};
</#list>

import java.util.*;
import com.n3twork.dynamap.*;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

public class ${updatesName} implements ${type.name}, Updates<${type.name}> {


    private final ${type.name} ${currentState};
    private final String hashKeyValue;
    private final Object rangeKeyValue;

<#list type.fields as field>
    private <@field_type field=field /> ${field.name};
    <#if field.multiValue??>
    <#if field.multiValue == 'LIST'>
    private <@field_type field=field /> ${field.name}Adds = new ArrayList();
    </#if>
    <#if field.multiValue == 'SET'>
    private <@field_type field=field /> ${field.name}Deletes = new HashSet();
    private <@field_type field=field /> ${field.name}Sets = new HashSet();
    </#if>
    <#if field.multiValue == 'MAP'>
    private Set<String> ${field.name}Deletes = new HashSet();
    private <@field_type field=field /> ${field.name}Sets = new HashMap();
    </#if>
    <#if field.multiValue == 'MAP' && field.isNumber()>
    private <@field_type field=field /> ${field.name}Deltas = new HashMap();
    </#if>
    <#elseif field.isNumber()>
    private ${field.type} ${field.name}Delta;
    </#if>
</#list>

    private final DynamoExpressionBuilder expression;

    public ${updatesName}(${type.name} ${currentState}, ObjectMapper objectMapper, String hashKeyValue, Object rangeKeyValue) {
        this.${currentState} = ${currentState};
        this.expression = new DynamoExpressionBuilder(objectMapper);
        this.hashKeyValue = hashKeyValue;
        this.rangeKeyValue = rangeKeyValue;
    }

    public ${updatesName}(${type.name} ${currentState}, ObjectMapper objectMapper, String hashKeyValue) {
        this(${currentState}, objectMapper, hashKeyValue, null);
    }

    @Override
    public String getTableName() {
        return "${tableName}";
    }

    @Override
    public DynamoExpressionBuilder withExpression() {
        return expression;
    }

    @Override
    public String getHashKeyValue() {
        return hashKeyValue;
    }

    @Override
    public Object getRangeKeyValue() {
        return rangeKeyValue;
    }

    ////// ${type.name} interface methods //////

<#list type.fields as field>
    <#if field.multiValue??>

    <#if field.multiValue == 'MAP'>
    @Override
    public Set<String> get${field.name?cap_first}Ids() {
        <#if field.isNumber()>
            <#assign deltas>${field.name}Deltas.keySet()</#assign>
        <#else>
            <#assign deltas>null</#assign>
        </#if>
        return MergeUtil.mergeUpdatesAndDeletes(${currentState}.get${field.name?cap_first}Ids(), ${deltas}, ${field.name}Sets.keySet(), ${field.name}Deletes);
    }
    @Override
    public ${field.type} get${field.name?cap_first}Value(String id) {
        <#if field.isNumber()>
        return MergeUtil.getLatestNumericValue(id, ${currentState}.get${field.name?cap_first}Value(id), ${field.name}Deltas, ${field.name}Sets);
        <#else>
        return MergeUtil.getLatestValue(id, ${currentState}.get${field.name?cap_first}Value(id), ${field.name}Sets, ${field.name}Deletes);
        </#if>
    }
    <#elseif field.multiValue == 'LIST'>
    @Override
    public List<${field.type}> get${field.name?cap_first}() {
        return MergeUtil.mergeAdds(${currentState}.get${field.name?cap_first}(), ${field.name}Adds);
    }
    <#elseif field.multiValue == 'SET'>
    @Override
    public Set<${field.type}> get${field.name?cap_first}() {
        return MergeUtil.mergeUpdatesAndDeletes(${currentState}.get${field.name?cap_first}(), null, ${field.name}Sets, ${field.name}Deletes);
    }
    </#if>

    <#else>
    @Override
    public ${field.type} get${field.name?cap_first}() {
        <#if field.isNumber()>
        return MergeUtil.getLatestNumericValue(${currentState}.get${field.name?cap_first}(), ${field.name}Delta, ${field.name});
        <#else>
        return this.${field.name} == null ? ${currentState}.get${field.name?cap_first}() : this.${field.name};
        </#if>
    }
    </#if>

</#list>

    /////// Mutator methods ///////////////////////

<#list type.fields as field>
    <#if field.multiValue! == 'MAP'>
        <#if field.isNumber()>
    public ${updatesName} increment${field.name?cap_first}Amount(String id, ${field.type} amount) {
        ${field.name}Deltas.put(id, ${field.name}Deltas.getOrDefault(id, ${field.defaultValue}) + amount);
        return this;
    }
        </#if>
    public ${updatesName} set${field.name?cap_first}Value(String id, ${field.type} value) {
        ${field.name}Sets.put(id, value);
        return this;
    }
    public ${updatesName} delete${field.name?cap_first}Value(String id) {
        ${field.name}Deletes.remove(id);
        return this;
    }
    <#elseif field.multiValue! == 'LIST'>
    public ${updatesName} add${field.name?cap_first}Value(${field.type} value) {
        ${field.name}Adds.add(value);
        return this;
    }
    public ${updatesName} set${field.name?cap_first}(List<${field.type}> list) {
        this.${field.name} = list;
        return this;
    }
    <#elseif field.multiValue! == 'SET'>
    public ${updatesName} setValue${field.name?cap_first}Value(${field.type} value) {
        ${field.name}Sets.add(value);
        return this;
    }
    public ${updatesName} delete${field.name?cap_first}Value(${field.type} value) {
        ${field.name}Deletes.remove(value);
        return this;
    }
    <#else>
    public ${updatesName} set${field.name?cap_first}(${field.type} value) {
        this.${field.name} = value;
        return this;
    }
    <#if field.isNumber()>
    public ${updatesName} increment${field.name?cap_first}(${field.type} amount) {
        ${field.name}Delta = (${field.name}Delta == null ? ${field.defaultValue} : ${field.name}Delta) + amount;
        return this;
    }
    </#if>
    </#if>
</#list>

    //////////////// Updates Interface Methods //////////

    @Override
    public String getUpdateExpression() {

        String parentDynamoFieldName = <#if isRoot>null;<#else>"${parentFieldName}";</#if>

    <#list type.fields as field>
        <#if field.multiValue! == 'MAP'>
            <#if field.isNumber()>
            <#if field.isReplace()>
            if ( ${field.name}Deltas.size() > 0 || ${field.name}Deletes.size() > 0) {
            Map<String, Long> allItems = new HashMap<>();
            for (String id : get${field.name}Ids()) {
                allItems.put(id, get${field.name}Value(id));
            }
            expression.setMultiValue(parentDynamoFieldName, "${field.dynamoName}", allItems, ${field.type}.class);
            <#else>
            expression.updateMap(parentDynamoFieldName, "${field.dynamoName}", ${field.name}Deltas, ${field.name}Sets, ${field.name}Deletes);
            </#if>
            <#elseif field.type != 'String'>
            expression.updateMap(parentDynamoFieldName, "${field.dynamoName}", null, ${field.name}Sets, ${field.name}Deletes);
            <#else>
            </#if>

        <#elseif field.multiValue! == 'LIST'>
            if (${field.name} != null) {
                expression.setMultiValue(parentDynamoFieldName, "${field.dynamoName}", ${field.name}, ${field.type}.class);
            }
            else {
                for (Object value : ${field.name}Adds) {
                    expression.incrementNumber(parentDynamoFieldName, "${field.dynamoName}", (Number) value);
                }
            }
        <#elseif field.multiValue! == 'SET'>
            if (${field.name} != null) {
                expression.setMultiValue(parentDynamoFieldName, "${field.dynamoName}", ${field.name}, ${field.type}.class);
            }
            else {
                for (Object value : ${field.name}Sets) {
                    expression.setValue(parentDynamoFieldName, "${field.dynamoName}", ${field.name}Sets);
                }
            }
        <#else>
            <#if field.isNumber()>
            if (${field.name} != null) {
                expression.setValue(parentDynamoFieldName, "${field.dynamoName}", ${field.name});
            }
            else if (${field.name}Delta != null) {
                expression.incrementNumber(parentDynamoFieldName, "${field.dynamoName}", ${field.name}Delta);
            }
            <#else>
            if (${field.name} != null) {
                expression.setValue(parentDynamoFieldName, "${field.dynamoName}", ${field.name});
            }
            </#if>
        </#if>
    </#list>

        return expression.buildUpdateExpression();
    }

    @Override
    public String getConditionalExpression() {
        expression.addCheckFieldValueCondition(null, "schemaVersion", ${rootType}.SCHEMA_VERSION);
        return expression.buildConditionalExpression();
    }
}