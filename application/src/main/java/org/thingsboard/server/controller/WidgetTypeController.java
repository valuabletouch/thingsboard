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
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class WidgetTypeController extends BaseController {
    public static final String TENANT_ID = "tenantId";

    @PreAuthorize("hasAnyAuthority('ROOT', 'SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetType/{widgetTypeId}", method = RequestMethod.GET)
    @ResponseBody
    public WidgetType getWidgetTypeById(
        @PathVariable("widgetTypeId") String strWidgetTypeId,
        @RequestParam(name = TENANT_ID, required = false) String requestTenantId)
        throws ThingsboardException {
        checkParameter("widgetTypeId", strWidgetTypeId);
        try {
            TenantId tenantId = getTenantId(requestTenantId);
            WidgetTypeId widgetTypeId = new WidgetTypeId(toUUID(strWidgetTypeId));
            return checkWidgetTypeId(widgetTypeId, Operation.READ, tenantId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetType", method = RequestMethod.POST)
    @ResponseBody
    public WidgetType saveWidgetType(
        @RequestParam(name = TENANT_ID, required = false) String requestTenantId,
        @RequestBody WidgetType widgetType)
        throws ThingsboardException {
        try {
            TenantId tenantId = getTenantId(requestTenantId);

            if (Authority.SYS_ADMIN.equals(getAuthority())) {
                widgetType.setTenantId(TenantId.SYS_TENANT_ID);
            } else {
                widgetType.setTenantId(tenantId);
            }

            checkEntity(widgetType.getId(), widgetType, Resource.WIDGET_TYPE, tenantId);

            return checkNotNull(widgetTypeService.saveWidgetType(widgetType));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetType/{widgetTypeId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteWidgetType(
        @PathVariable("widgetTypeId") String strWidgetTypeId,
        @RequestParam(name = TENANT_ID, required = false) String requestTenantId)
        throws ThingsboardException {
        checkParameter("widgetTypeId", strWidgetTypeId);
        try {
            TenantId tenantId = getTenantId(requestTenantId);
            WidgetTypeId widgetTypeId = new WidgetTypeId(toUUID(strWidgetTypeId));
            checkWidgetTypeId(widgetTypeId, Operation.DELETE, tenantId);
            widgetTypeService.deleteWidgetType(tenantId,widgetTypeId);
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
        @RequestParam(name = TENANT_ID, required = false) String requestTenantId)
        throws ThingsboardException {
        try {
            TenantId tenantId = getTenantId(requestTenantId);

            if (isSystem && Authority.SYS_ADMIN.equals(getAuthority())) {
                tenantId = TenantId.SYS_TENANT_ID;
            } 
            return checkNotNull(widgetTypeService.findWidgetTypesByTenantIdAndBundleAlias(tenantId, bundleAlias));
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
            @RequestParam(name = TENANT_ID, required = false) String requestTenantId) throws ThingsboardException {
        try {
            TenantId tenantId = getTenantId(requestTenantId);

            if (isSystem && Authority.SYS_ADMIN.equals(getAuthority())) {
                tenantId = TenantId.SYS_TENANT_ID;
            } 

            WidgetType widgetType = widgetTypeService.findWidgetTypeByTenantIdBundleAliasAndAlias(tenantId, bundleAlias, alias);
            checkNotNull(widgetType);
            accessControlService.checkPermission(getCurrentUser(), Resource.WIDGET_TYPE, Operation.READ, widgetType.getId(), widgetType);

            return widgetType;
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
