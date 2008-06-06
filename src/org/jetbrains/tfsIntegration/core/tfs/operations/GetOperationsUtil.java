/*
 * Copyright 2000-2008 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.tfsIntegration.core.tfs.operations;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GetOperationsUtil {
  static List<GetOperation> sortGetOperations(Collection<GetOperation> getOperations) {
    List<GetOperation> result = new ArrayList<GetOperation>(getOperations.size());
    for (GetOperation newOperation : getOperations) {
      TFSVcs.assertTrue(newOperation.getSlocal() != null || newOperation.getTlocal() != null);
      int positionToInsert = result.size();
      if (newOperation.getSlocal() != null) {
        final FilePath newOpPath = VcsUtil.getFilePath(newOperation.getSlocal());
        for (int i = 0; i < result.size(); i++) {
          final GetOperation existingOperation = result.get(i);
          if (existingOperation.getSlocal() == null || VcsUtil.getFilePath(existingOperation.getSlocal()).isUnder(newOpPath, false)) {
            positionToInsert = i;
            break;
          }
        }
      }
      result.add(positionToInsert, newOperation);
    }
    return result;
  }

  public static void updateSourcePaths(final List<GetOperation> sortedOperations, final int index, final GetOperation operation) {
    // TODO: replaceFirst to handle unix paths: problem if replace /a -> /aa in /a/a/a
    for (GetOperation operationToUpdate : sortedOperations.subList(index + 1, sortedOperations.size())) {
      if (operationToUpdate.getSlocal() != null) {
        final String updated = operationToUpdate.getSlocal().replace(operation.getSlocal(), operation.getTlocal());
        operationToUpdate.setSlocal(updated);
      }
    }
  }
}