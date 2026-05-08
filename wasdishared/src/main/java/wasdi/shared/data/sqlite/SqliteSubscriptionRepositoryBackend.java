package wasdi.shared.data.sqlite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wasdi.shared.business.Organization;
import wasdi.shared.business.Subscription;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.data.OrganizationRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.data.interfaces.ISubscriptionRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * SQLite backend implementation for subscription repository.
 */
public class SqliteSubscriptionRepositoryBackend extends SqliteRepository implements ISubscriptionRepositoryBackend {

    public SqliteSubscriptionRepositoryBackend() {
        m_sThisCollection = "subscriptions";
        this.ensureTable(m_sThisCollection);
    }

    @Override
    public boolean insertSubscription(Subscription oSubscription) {
        try {
            if (oSubscription == null || Utils.isNullOrEmpty(oSubscription.getSubscriptionId())) {
                return false;
            }
            return insert(oSubscription.getSubscriptionId(), oSubscription);
        } catch (Exception oEx) {
            WasdiLog.errorLog("SubscriptionRepository.insertSubscription: error " + oEx.toString());
        }

        return false;
    }

    @Override
    public boolean updateSubscription(Subscription oSubscription) {
        try {
            if (oSubscription == null || Utils.isNullOrEmpty(oSubscription.getSubscriptionId())) {
                return false;
            }
            return updateById(oSubscription.getSubscriptionId(), oSubscription);
        } catch (Exception oEx) {
            WasdiLog.errorLog("SubscriptionRepository.updateSubscription: error " + oEx.toString());
        }

        return false;
    }

    @Override
    public Subscription getSubscriptionById(String sSubscriptionId) {
        try {
            return findOneWhere("subscriptionId", sSubscriptionId, Subscription.class);
        } catch (Exception oEx) {
            WasdiLog.errorLog("SubscriptionRepository.getSubscriptionById: error " + oEx.toString());
        }

        return null;
    }

    @Override
    public List<Subscription> getSubscriptionsBySubscriptionIds(Collection<String> asSubscriptionIds) {
        final List<Subscription> aoReturnList = new ArrayList<>();

        try {
            if (asSubscriptionIds == null || asSubscriptionIds.isEmpty()) {
                return aoReturnList;
            }

            StringBuilder oSql = new StringBuilder("SELECT data FROM ").append(m_sThisCollection)
                    .append(" WHERE json_extract(data,'$.subscriptionId') IN (");
            for (int i = 0; i < asSubscriptionIds.size(); i++) {
                if (i > 0) oSql.append(',');
                oSql.append('?');
            }
            oSql.append(')');

            return queryList(oSql.toString(), asSubscriptionIds.toArray(), Subscription.class);
        } catch (Exception oEx) {
            WasdiLog.errorLog("SubscriptionRepository.getSubscriptionsBySubscriptionIds : error " + oEx.toString());
        }

        return aoReturnList;
    }

    @Override
    public boolean checkValidSubscriptionBySubscriptionId(String sSubscriptionId) {
        if (Utils.isNullOrEmpty(sSubscriptionId)) {
            return false;
        }

        Subscription oSubscription = this.getSubscriptionById(sSubscriptionId);

        if (oSubscription == null) {
            return false;
        }

        return oSubscription.isValid();
    }

    @Override
    public List<Subscription> getSubscriptionsByUser(String sUserId) {
        try {
            return findAllWhere("userId", sUserId, Subscription.class);
        } catch (Exception oEx) {
            WasdiLog.errorLog("SubscriptionRepository.checkValidSubscription : error " + oEx.toString());
        }

        return new ArrayList<>();
    }

    @Override
    public List<Subscription> getSubscriptionsByOrganization(String sOrganizationId) {
        try {
            return findAllWhere("organizationId", sOrganizationId, Subscription.class);
        } catch (Exception oEx) {
            WasdiLog.errorLog("SubscriptionRepository.getSubscriptionsByOrganization : error " + oEx.toString());
        }

        return new ArrayList<>();
    }

    @Override
    public boolean organizationHasSubscriptions(String sOrganizationId) {
        try {
            return countWhere("organizationId", sOrganizationId) > 0;
        } catch (Exception oEx) {
            WasdiLog.errorLog("SubscriptionRepository.organizationHasSubscriptions : error " + oEx.toString());
        }

        // Keep Mongo behavior on error.
        return true;
    }

    @Override
    public List<Subscription> getSubscriptionsByOrganizations(Collection<String> asOrganizationIds) {
        final List<Subscription> aoReturnList = new ArrayList<>();

        try {
            if (asOrganizationIds == null || asOrganizationIds.isEmpty()) {
                return aoReturnList;
            }

            StringBuilder oSql = new StringBuilder("SELECT data FROM ").append(m_sThisCollection)
                    .append(" WHERE json_extract(data,'$.organizationId') IN (");
            for (int i = 0; i < asOrganizationIds.size(); i++) {
                if (i > 0) oSql.append(',');
                oSql.append('?');
            }
            oSql.append(')');

            return queryList(oSql.toString(), asOrganizationIds.toArray(), Subscription.class);
        } catch (Exception oEx) {
            WasdiLog.errorLog("SubscriptionRepository.getSubscriptionsByOrganizations : error " + oEx.toString());
        }

        return aoReturnList;
    }

    @Override
    public Subscription getByName(String sName) {
        try {
            return findOneWhere("name", sName, Subscription.class);
        } catch (Exception oE) {
            WasdiLog.errorLog("SubscriptionRepository.getByName( " + sName + "): error: ", oE);
        }

        return null;
    }

    @Override
    public Subscription getByNameAndUserId(String sName, String sUserId) {
        try {
            Map<String, Object> oFilter = new HashMap<>();
            oFilter.put("userId", sUserId);
            oFilter.put("name", sName);
            return findOneWhere(oFilter, Subscription.class);
        } catch (Exception oE) {
            WasdiLog.errorLog("SubscriptionRepository.getByName: error: ", oE);
        }

        return null;
    }

    @Override
    public boolean deleteSubscription(String sSubscriptionId) {
        if (Utils.isNullOrEmpty(sSubscriptionId)) {
            return false;
        }

        try {
            return deleteById(sSubscriptionId) > 0;
        } catch (Exception oEx) {
            WasdiLog.errorLog("SubscriptionRepository.deleteSubscription : error " + oEx.toString());
        }

        return false;
    }

    @Override
    public int deleteByUser(String sUserId) {
        if (Utils.isNullOrEmpty(sUserId)) {
            return 0;
        }

        try {
            return deleteWhere("userId", sUserId);
        } catch (Exception oEx) {
            WasdiLog.errorLog("SubscriptionRepository.deleteByUser : error " + oEx.toString());
        }

        return 0;
    }

    @Override
    public boolean isOwnedByUser(String sUserId, String sSubscriptionId) {
        try {
            Map<String, Object> oFilter = new HashMap<>();
            oFilter.put("userId", sUserId);
            oFilter.put("subscriptionId", sSubscriptionId);
            return countWhere(oFilter) > 0;
        } catch (Exception oE) {
            WasdiLog.errorLog("SubscriptionRepository.isOwnedByUser( " + sUserId + ", " + sSubscriptionId + " ): error: ", oE);
        }

        return false;
    }

    @Override
    public List<Subscription> getSubscriptionsList() {
        try {
            return findAll(Subscription.class);
        } catch (Exception oEx) {
            WasdiLog.errorLog("SubscriptionRepository.getSubscriptionsList : error " + oEx.toString());
        }

        return new ArrayList<>();
    }

    @Override
    public long getSubscriptionsCount() {

        try {
            return count();
        } catch (Exception oEx) {
            WasdiLog.errorLog("SubscriptionRepository.getSubscriptionsCount : error " + oEx.toString());
        }

        return -1L;
    }

    @Override
    public List<Subscription> getSubscriptionsSortedList(String sOrderBy, int iOrder) {
        final List<Subscription> aoReturnList = new ArrayList<>();

        if (Utils.isNullOrEmpty(sOrderBy)) {
            sOrderBy = "name";
        }

        if (iOrder != -1 && iOrder != 1) {
            iOrder = 1;
        }

        try {
            String sField = sanitizeOrderField(sOrderBy);
            String sDir = iOrder == -1 ? "DESC" : "ASC";
            return queryList(
                    "SELECT data FROM " + m_sThisCollection +
                    " ORDER BY json_extract(data,'$." + sField + "') " + sDir,
                    new Object[]{}, Subscription.class);
        } catch (Exception oEx) {
            WasdiLog.errorLog("SubscriptionRepository.getSubscriptionsSortedList : error ", oEx);
        }

        return aoReturnList;
    }

    @Override
    public List<Subscription> findSubscriptionsByFilters(String sNameFilter, String sIdFilter, String sUserIdFilter, String sOrderBy, int iOrder) {

        List<Subscription> aoReturnList = new ArrayList<>();

        if (Utils.isNullOrEmpty(sOrderBy)) {
            sOrderBy = "name";
        }

        if (iOrder != -1 && iOrder != 1) {
            iOrder = 1;
        }

        List<String> asClauses = new ArrayList<>();
        List<Object> aoParams = new ArrayList<>();

        if (!Utils.isNullOrEmpty(sNameFilter)) {
            asClauses.add("LOWER(json_extract(data,'$.name')) LIKE LOWER(?)");
            aoParams.add("%" + sNameFilter + "%");
        }

        if (!Utils.isNullOrEmpty(sIdFilter)) {
            asClauses.add("LOWER(json_extract(data,'$.subscriptionId')) LIKE LOWER(?)");
            aoParams.add("%" + sIdFilter + "%");
        }

        if (!Utils.isNullOrEmpty(sUserIdFilter)) {
            asClauses.add("LOWER(json_extract(data,'$.userId')) LIKE LOWER(?)");
            aoParams.add("%" + sUserIdFilter + "%");
        }

        if (asClauses.isEmpty()) {
            return getSubscriptionsSortedList(sOrderBy, iOrder);
        }

        try {
            String sField = sanitizeOrderField(sOrderBy);
            String sDir = iOrder == -1 ? "DESC" : "ASC";
            StringBuilder oSql = new StringBuilder("SELECT data FROM ").append(m_sThisCollection).append(" WHERE ");
            for (int i = 0; i < asClauses.size(); i++) {
                if (i > 0) oSql.append(" OR ");
                oSql.append(asClauses.get(i));
            }
            oSql.append(" ORDER BY json_extract(data,'$.").append(sField).append("') ").append(sDir);

            return queryList(oSql.toString(), aoParams.toArray(), Subscription.class);
        } catch (Exception oEx) {
            WasdiLog.errorLog("SubscriptionRepository.findSubscriptionsByFilters : error ", oEx);
        }

        return aoReturnList;
    }

    @Override
    public List<Subscription> getAllSubscriptionsOfUser(String sUserId) {

        final List<Subscription> aoReturnList = new ArrayList<>();
        if (Utils.isNullOrEmpty(sUserId)) {
            return aoReturnList;
        }

        try {
            aoReturnList.addAll(findAllWhere("userId", sUserId, Subscription.class));
        } catch (Exception oEx) {
            WasdiLog.errorLog("SubscriptionRepository.getAllSubscriptionsOfUser : error ", oEx);
        }

        try {

            OrganizationRepository oOrganizationRepository = new OrganizationRepository();

            List<Organization> aoOwnedOrgs = oOrganizationRepository.getOrganizationsOwnedByUser(sUserId);

            UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
            List<UserResourcePermission> aoSharedOrgs = oUserResourcePermissionRepository.getOrganizationSharingsByUserId(sUserId);

            for (UserResourcePermission oSharedOrg : aoSharedOrgs) {

                boolean bFound = false;

                for (Organization oOrganization : aoOwnedOrgs) {
                    if (oOrganization.getOrganizationId().equals(oSharedOrg.getResourceId())) {
                        bFound = true;
                        break;
                    }
                }

                if (!bFound) {
                    Organization oToAdd = oOrganizationRepository.getById(oSharedOrg.getResourceId());
                    aoOwnedOrgs.add(oToAdd);
                }
            }

            ArrayList<String> asOrgsId = new ArrayList<>();
            for (Organization oOrganization : aoOwnedOrgs) {
                asOrgsId.add(oOrganization.getOrganizationId());
            }

            List<Subscription> aoSubscriptionsSharedWithUser = this.getSubscriptionsByOrganizations(asOrgsId);

            List<UserResourcePermission> aoSharedSub = oUserResourcePermissionRepository.getSubscriptionSharingsByUserId(sUserId);

            for (UserResourcePermission oSharedSub : aoSharedSub) {

                boolean bFound = false;

                for (Subscription oSubscription : aoSubscriptionsSharedWithUser) {
                    if (oSubscription.getSubscriptionId().equals(oSharedSub.getResourceId())) {
                        bFound = true;
                        break;
                    }
                }

                if (!bFound) {
                    Subscription oToAdd = this.getSubscriptionById(oSharedSub.getResourceId());
                    aoSubscriptionsSharedWithUser.add(oToAdd);
                }
            }

            for (Subscription oSharedSub : aoSubscriptionsSharedWithUser) {

                boolean bFound = false;

                for (Subscription oSubscription : aoReturnList) {
                    if (oSubscription.getSubscriptionId().equals(oSharedSub.getSubscriptionId())) {
                        bFound = true;
                        break;
                    }
                }

                if (!bFound) {
                    aoReturnList.add(oSharedSub);
                }
            }

        } catch (Exception oEx) {
            WasdiLog.errorLog("SubscriptionRepository.getAllSubscriptionsOfUser : error searching not-owned subscriptions " + oEx.toString());
        }

        return aoReturnList;
    }

    @Override
    public List<Subscription> findSubscriptionByPartialName(String sPartialName) {

        List<Subscription> aoReturnList = new ArrayList<>();

        if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
            return aoReturnList;
        }

        try {
            String sLike = "%" + sPartialName + "%";
            return queryList(
                    "SELECT data FROM " + m_sThisCollection +
                    " WHERE LOWER(json_extract(data,'$.subscriptionId')) LIKE LOWER(?)" +
                    " OR LOWER(json_extract(data,'$.name')) LIKE LOWER(?)" +
                    " ORDER BY json_extract(data,'$.name') ASC",
                    new Object[]{sLike, sLike},
                    Subscription.class);
        } catch (Exception oEx) {
            WasdiLog.errorLog("SubscriptionRepository.findSubscriptionByPartialName: error: ", oEx);
        }

        return aoReturnList;
    }

    private String sanitizeOrderField(String sOrderBy) {
        // Keep sorting flexible but block malformed SQL fragments.
        if (Utils.isNullOrEmpty(sOrderBy)) {
            return "name";
        }
        if (!sOrderBy.matches("[A-Za-z0-9_]+")) {
            return "name";
        }
        return sOrderBy;
    }
}
