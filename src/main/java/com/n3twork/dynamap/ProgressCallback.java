/*
    Copyright 2018 N3TWORK INC

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

public interface ProgressCallback {

    /**
     * Report progress back to the caller. The caller can return a signal to cancel the request.
     * @param progressCount a number to indicate the progress made
     * @return When used with batchGetObject, true if process should continue, false if the process should be cancelled. Otherwise this value is not used.
     */
    boolean reportProgress(int progressCount);

}
