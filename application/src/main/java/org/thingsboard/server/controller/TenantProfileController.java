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

import lombok.extern.slf4j.Slf4j;
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
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class TenantProfileController extends BaseController {
    public static final String TENANT_ID = "tenantId";

    @PreAuthorize("hasAnyAuthority('ROOT', 'SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfile/{tenantProfileId}", method = RequestMethod.GET)
    @ResponseBody
    public TenantProfile getTenantProfileById(
        @PathVariable("tenantProfileId") String strTenantProfileId,
        @RequestParam(name = TENANT_ID, required = false) String requestTenantId)
        throws ThingsboardException {
        checkParameter("tenantProfileId", strTenantProfileId);
        try {
            TenantId tenantId = getTenantId(requestTenantId);
            TenantProfileId tenantProfileId = new TenantProfileId(toUUID(strTenantProfileId));
            return checkTenantProfileId(tenantProfileId, Operation.READ, tenantId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfileInfo/{tenantProfileId}", method = RequestMethod.GET)
    @ResponseBody
    public EntityInfo getTenantProfileInfoById(
        @PathVariable("tenantProfileId") String strTenantProfileId,
        @RequestParam(name = TENANT_ID, required = false) String requestTenantId)
        throws ThingsboardException {
        checkParameter("tenantProfileId", strTenantProfileId);
        try {
            TenantId tenantId = getTenantId(requestTenantId);
            TenantProfileId tenantProfileId = new TenantProfileId(toUUID(strTenantProfileId));
            return checkNotNull(tenantProfileService.findTenantProfileInfoById(tenantId, tenantProfileId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfileInfo/default", method = RequestMethod.GET)
    @ResponseBody
    public EntityInfo getDefaultTenantProfileInfo(@RequestParam(name = TENANT_ID, required = false) String requestTenantId) throws ThingsboardException {
        try {
            TenantId tenantId = getTenantId(requestTenantId);
            return checkNotNull(tenantProfileService.findDefaultTenantProfileInfo(tenantId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfile", method = RequestMethod.POST)
    @ResponseBody
    public TenantProfile saveTenantProfile(
        @RequestParam(name = TENANT_ID, required = false) String requestTenantId,
        @RequestBody TenantProfile tenantProfile)
        throws ThingsboardException {
        try {
            TenantId tenantId = getTenantId(requestTenantId);
            boolean newTenantProfile = tenantProfile.getId() == null;

            if (newTenantProfile) {
                accessControlService.checkPermission(getCurrentUser(), Resource.TENANT_PROFILE, Operation.CREATE);
            } else {
                checkEntityId(tenantProfile.getId(), Operation.WRITE, tenantId);
            }

            tenantProfile = checkNotNull(tenantProfileService.saveTenantProfile(tenantId, tenantProfile));
            tenantProfileCache.put(tenantProfile);
            tbClusterService.onTenantProfileChange(tenantProfile, null);
            tbClusterService.onEntityStateChange(TenantId.SYS_TENANT_ID, tenantProfile.getId(), newTenantProfile ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
            return tenantProfile;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfile/{tenantProfileId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteTenantProfile(
        @PathVariable("tenantProfileId") String strTenantProfileId,
        @RequestParam(name = TENANT_ID, required = false) String requestTenantId)
        throws ThingsboardException {
        checkParameter("tenantProfileId", strTenantProfileId);
        try {
            TenantId tenantId = getTenantId(requestTenantId);
            TenantProfileId tenantProfileId = new TenantProfileId(toUUID(strTenantProfileId));
            TenantProfile profile = checkTenantProfileId(tenantProfileId, Operation.DELETE, tenantId);
            tenantProfileService.deleteTenantProfile(tenantId, tenantProfileId);
            tbClusterService.onTenantProfileDelete(profile, null);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfile/{tenantProfileId}/default", method = RequestMethod.POST)
    @ResponseBody
    public TenantProfile setDefaultTenantProfile(
        @PathVariable("tenantProfileId") String strTenantProfileId,
        @RequestParam(name = TENANT_ID, required = false) String requestTenantId)
        throws ThingsboardException {
        checkParameter("tenantProfileId", strTenantProfileId);
        try {
            TenantId tenantId = getTenantId(requestTenantId);
            TenantProfileId tenantProfileId = new TenantProfileId(toUUID(strTenantProfileId));
            TenantProfile tenantProfile = checkTenantProfileId(tenantProfileId, Operation.WRITE, tenantId);
            tenantProfileService.setDefaultTenantProfile(tenantId, tenantProfileId);
            return tenantProfile;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfiles", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<TenantProfile> getTenantProfiles(
        @RequestParam int pageSize,
        @RequestParam int page,
        @RequestParam(required = false) String textSearch,
        @RequestParam(required = false) String sortProperty,
        @RequestParam(required = false) String sortOrder,
        @RequestParam(name = TENANT_ID, required = false) String requestTenantId)
        throws ThingsboardException {
        try {
            TenantId tenantId = getTenantId(requestTenantId);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(tenantProfileService.findTenantProfiles(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfileInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EntityInfo> getTenantProfileInfos(
        @RequestParam int pageSize,
        @RequestParam int page,
        @RequestParam(required = false) String textSearch,
        @RequestParam(required = false) String sortProperty,
        @RequestParam(required = false) String sortOrder,
        @RequestParam(name = TENANT_ID, required = false) String requestTenantId)
        throws ThingsboardException {
        try {
            TenantId tenantId = getTenantId(requestTenantId);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(tenantProfileService.findTenantProfileInfos(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
