/*
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
*/

package com.n3twork.dynamap;

public class GetObjectRequest<T extends DynamapRecordBean> {

    private String hashKeyValue;
    private Object rangeKeyValue;
    private boolean consistentRead;
    private final Class<T> resultClass;
    private String suffix;

    public GetObjectRequest(Class<T> resultClass) {
        this.resultClass = resultClass;
    }

    public GetObjectRequest<T> withHashKeyValue(String hashKeyValue) {
        this.hashKeyValue = hashKeyValue;
        return this;
    }

    public GetObjectRequest<T> withRangeKeyValue(Object rangeKeyValue) {
        this.rangeKeyValue = rangeKeyValue;
        return this;
    }

    public GetObjectRequest<T> withConsistentRead(boolean consistentRead) {
        this.consistentRead = consistentRead;
        return this;
    }

    public GetObjectRequest<T> withSuffix(String suffix) {
        this.suffix = suffix;
        return this;
    }

    public String getHashKeyValue() {
        return hashKeyValue;
    }

    public Object getRangeKeyValue() {
        return rangeKeyValue;
    }

    public boolean isConsistentRead() {
        return consistentRead;
    }

    public Class<T> getResultClass() {
        return resultClass;
    }

    public String getSuffix() {
        return suffix;
    }
}
