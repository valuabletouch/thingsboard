/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class WidgetTypeController extends BaseController {

    @PreAuthorize("hasAnyAuthority('ROOT', 'SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetType/{widgetTypeId}", method = RequestMethod.GET)
    @ResponseBody
    public WidgetType getWidgetTypeById(@PathVariable("widgetTypeId") String strWidgetTypeId) throws ThingsboardException {
        checkParameter("widgetTypeId", strWidgetTypeId);
        try {
            WidgetTypeId widgetTypeId = new WidgetTypeId(toUUID(strWidgetTypeId));
            return checkWidgetTypeId(widgetTypeId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetType", method = RequestMethod.POST)
    @ResponseBody
    public WidgetType saveWidgetType(@RequestBody WidgetType widgetType,@RequestParam(name = "tenantId", required = false) TenantId tenantId) throws ThingsboardException {
        try {
            TenantId currentTenantId =
                getAuthority() == Authority.ROOT && tenantId != null
                    ? tenantId
                    : getTenantId();

            if (Authority.SYS_ADMIN.equals(getCurrentUser().getAuthority())) {
                widgetType.setTenantId(TenantId.SYS_TENANT_ID);
            } else {
                widgetType.setTenantId(currentTenantId);
            }

            checkEntity(widgetType.getId(), widgetType, Resource.WIDGET_TYPE);

            return checkNotNull(widgetTypeService.saveWidgetType(widgetType));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetType/{widgetTypeId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteWidgetType(@PathVariable("widgetTypeId") String strWidgetTypeId, @RequestParam(name = "tenantId", required = false) TenantId tenantId) throws ThingsboardException {
        checkParameter("widgetTypeId", strWidgetTypeId);
        try {
            TenantId currentTenantId =
                getAuthority() == Authority.ROOT && tenantId != null
                    ? tenantId
                    : getTenantId();

            WidgetTypeId widgetTypeId = new WidgetTypeId(toUUID(strWidgetTypeId));
            checkWidgetTypeId(widgetTypeId, Operation.DELETE);
            widgetTypeService.deleteWidgetType(currentTenantId,widgetTypeId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetTypes", params = {"isSystem", "bundleAlias"}, method = RequestMethod.GET)
    @ResponseBody
    public List<WidgetType> getBundleWidgetTypes(
            @RequestParam boolean isSystem,
            @RequestParam String bundleAlias,
            @RequestParam(name = "tenantId", required = false) TenantId tenantId) throws ThingsboardException {
        try {
            TenantId currentTenantId =
                getAuthority() == Authority.ROOT && tenantId != null
                    ? tenantId
                    : getTenantId();

            if (isSystem && getAuthority() == Authority.SYS_ADMIN) {
                currentTenantId = TenantId.SYS_TENANT_ID;
            } 
            return checkNotNull(widgetTypeService.findWidgetTypesByTenantIdAndBundleAlias(currentTenantId, bundleAlias));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/widgetType", params = {"isSystem", "bundleAlias", "alias"}, method = RequestMethod.GET)
    @ResponseBody
    public WidgetType getWidgetType(
            @RequestParam boolean isSystem,
            @RequestParam String bundleAlias,
            @RequestParam String alias,
            @RequestParam(name = "tenantId", required = false) TenantId tenantId) throws ThingsboardException {
        try {
            TenantId currentTenantId =
                getAuthority() == Authority.ROOT && tenantId != null
                    ? tenantId
                    : getTenantId();

            if (isSystem && getAuthority() == Authority.SYS_ADMIN) {
                currentTenantId = new TenantId(ModelConstants.NULL_UUID);
            } 
            WidgetType widgetType = widgetTypeService.findWidgetTypeByTenantIdBundleAliasAndAlias(currentTenantId, bundleAlias, alias);
            checkNotNull(widgetType);
            accessControlService.checkPermission(getCurrentUser(), Resource.WIDGET_TYPE, Operation.READ, widgetType.getId(), widgetType);
            return widgetType;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
