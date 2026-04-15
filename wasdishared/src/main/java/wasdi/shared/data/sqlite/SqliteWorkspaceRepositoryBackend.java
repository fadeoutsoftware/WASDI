package wasdi.shared.data.sqlite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wasdi.shared.business.Workspace;
import wasdi.shared.data.interfaces.IWorkspaceRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * SQLite backend implementation for workspace repository.
 */
public class SqliteWorkspaceRepositoryBackend extends SqliteRepository implements IWorkspaceRepositoryBackend {

    public SqliteWorkspaceRepositoryBackend() {
        m_sThisCollection = "workspaces";
        this.ensureTable(m_sThisCollection);
    }

    @Override
    public boolean insertWorkspace(Workspace oWorkspace) {

        try {
            if (oWorkspace == null || Utils.isNullOrEmpty(oWorkspace.getWorkspaceId())) {
                return false;
            }

            return insert(oWorkspace.getWorkspaceId(), oWorkspace);

        } catch (Exception oEx) {
            WasdiLog.errorLog("WorkspaceRepository.insertWorkspace: error: ", oEx);
        }

        return false;
    }

    @Override
    public boolean updateWorkspaceName(Workspace oWorkspace) {

        try {
            if (oWorkspace == null || Utils.isNullOrEmpty(oWorkspace.getWorkspaceId())) {
                return false;
            }

            Workspace oStored = getWorkspace(oWorkspace.getWorkspaceId());
            if (oStored == null) {
                return false;
            }

            oStored.setName(oWorkspace.getName());
            return updateById(oStored.getWorkspaceId(), oStored);

        } catch (Exception oEx) {
            WasdiLog.errorLog("WorkspaceRepository.updateWorkspaceName: error: ", oEx);
        }

        return false;
    }

    @Override
    public boolean updateWorkspacePublicFlag(Workspace oWorkspace) {

        try {
            if (oWorkspace == null || Utils.isNullOrEmpty(oWorkspace.getWorkspaceId())) {
                return false;
            }

            Workspace oStored = getWorkspace(oWorkspace.getWorkspaceId());
            if (oStored == null) {
                return false;
            }

            oStored.setPublic(oWorkspace.isPublic());
            return updateById(oStored.getWorkspaceId(), oStored);

        } catch (Exception oEx) {
            WasdiLog.errorLog("WorkspaceRepository.updateWorkspacePublicFlag: error: ", oEx);
        }

        return false;
    }

    @Override
    public boolean updateWorkspaceNodeCode(Workspace oWorkspace) {

        try {
            if (oWorkspace == null || Utils.isNullOrEmpty(oWorkspace.getWorkspaceId())) {
                return false;
            }

            Workspace oStored = getWorkspace(oWorkspace.getWorkspaceId());
            if (oStored == null) {
                return false;
            }

            oStored.setNodeCode(oWorkspace.getNodeCode());
            return updateById(oStored.getWorkspaceId(), oStored);

        } catch (Exception oEx) {
            WasdiLog.errorLog("WorkspaceRepository.updateWorkspaceNodeCode: error: ", oEx);
        }

        return false;
    }

    @Override
    public boolean updateWorkspace(Workspace oWorkspace) {

        try {
            if (oWorkspace == null || Utils.isNullOrEmpty(oWorkspace.getWorkspaceId())) {
                return false;
            }

            return updateById(oWorkspace.getWorkspaceId(), oWorkspace);

        } catch (Exception oEx) {
            WasdiLog.errorLog("WorkspaceRepository.updateWorkspace: error: ", oEx);
        }

        return false;
    }

    @Override
    public Workspace getWorkspace(String sWorkspaceId) {

        try {
            return findOneWhere("workspaceId", sWorkspaceId, Workspace.class);

        } catch (Exception oEx) {
            WasdiLog.errorLog("WorkspaceRepository.getWorkspace: error: ", oEx);
        }

        return null;
    }

    @Override
    public List<Workspace> getWorkspaceByUser(String sUserId) {

        try {
            return findAllWhere("userId", sUserId, Workspace.class);

        } catch (Exception oEx) {
            WasdiLog.errorLog("WorkspaceRepository.getWorkspaceByUser: error: ", oEx);
        }

        return new ArrayList<>();
    }

    @Override
    public List<Workspace> getWorkspaceByNode(String sNodeCode) {

        try {
            return findAllWhere("nodeCode", sNodeCode, Workspace.class);

        } catch (Exception oEx) {
            WasdiLog.errorLog("WorkspaceRepository.getWorkspaceByNode: error: ", oEx);
        }

        return new ArrayList<>();
    }

    @Override
    public List<Workspace> getWorkspacesSortedByOldestUpdate(String sUserId) {

        try {
            return queryList(
                    "SELECT data FROM " + m_sThisCollection +
                    " WHERE json_extract(data,'$.userId') = ?" +
                    " ORDER BY CAST(json_extract(data,'$.lastEditDate') AS REAL) ASC",
                    new Object[]{sUserId}, Workspace.class);

        } catch (Exception oEx) {
            WasdiLog.errorLog("WorkspaceRepository.getWorkspacesFromOldestUpdate: error: ", oEx);
        }

        return new ArrayList<>();
    }

    @Override
    public Workspace getByUserIdAndWorkspaceName(String sUserId, String sName) {
        try {
            Map<String, Object> oFilter = new HashMap<>();
            oFilter.put("userId", sUserId);
            oFilter.put("name", sName);
            return findOneWhere(oFilter, Workspace.class);

        } catch (Exception oE) {
            WasdiLog.errorLog("WorkspaceRepository.getByUserIdAndWorkspaceName error: ", oE);
        }

        return null;
    }

    @Override
    public Workspace getByNameAndNode(String sName, String sNode) {
        try {
            Map<String, Object> oFilter = new HashMap<>();
            oFilter.put("name", sName);
            oFilter.put("nodeCode", sNode);
            return findOneWhere(oFilter, Workspace.class);

        } catch (Exception oE) {
            WasdiLog.errorLog("WorkspaceRepository.getByNameAndNode error: ", oE);
        }

        return null;
    }

    @Override
    public boolean deleteWorkspace(String sWorkspaceId) {

        if (Utils.isNullOrEmpty(sWorkspaceId)) {
            return false;
        }

        try {
            return deleteById(sWorkspaceId) > 0;

        } catch (Exception oEx) {
            WasdiLog.errorLog("WorkspaceRepository.deleteWorkspace: error: ", oEx);
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
            WasdiLog.errorLog("WorkspaceRepository.deleteByUser: error: ", oEx);
        }

        return 0;
    }

    @Override
    public boolean isOwnedByUser(String sUserId, String sWorkspaceId) {
        try {
            Map<String, Object> oFilter = new HashMap<>();
            oFilter.put("userId", sUserId);
            oFilter.put("workspaceId", sWorkspaceId);
            return countWhere(oFilter) > 0;
        } catch (Exception oE) {
            WasdiLog.errorLog("WorkspaceRepository.isOwnedByUser error: " + oE);
        }
        return false;
    }

    @Override
    public List<Workspace> getWorkspacesList() {

        try {
            return findAll(Workspace.class);

        } catch (Exception oEx) {
            WasdiLog.errorLog("WorkspaceRepository.getWorkspacesList: error: ", oEx);
        }

        return new ArrayList<>();
    }

    @Override
    public List<Workspace> findWorkspacesByPartialName(String sPartialName) {
        List<Workspace> aoReturnList = new ArrayList<>();

        if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
            return aoReturnList;
        }

        try {
            String sLike = "%" + sPartialName + "%";
            return queryList(
                    "SELECT data FROM " + m_sThisCollection +
                    " WHERE LOWER(json_extract(data,'$.workspaceId')) LIKE LOWER(?)" +
                    " OR LOWER(json_extract(data,'$.name')) LIKE LOWER(?)" +
                    " ORDER BY json_extract(data,'$.name') ASC",
                    new Object[]{sLike, sLike}, Workspace.class);
        } catch (Exception oEx) {
            WasdiLog.errorLog("WorkspaceRepository.findWorkspacesByPartialName: error: ", oEx);
        }

        return aoReturnList;
    }

    @Override
    public Long getStorageUsageForUser(String sUserId) {

        Long lStorageUsage = -1L;

        if (Utils.isNullOrEmpty(sUserId)) {
            return lStorageUsage;
        }

        try {
            try (java.sql.Connection oConn = getConnection();
                 java.sql.PreparedStatement oPs = oConn.prepareStatement(
                         "SELECT SUM(CAST(COALESCE(json_extract(data,'$.storageSize'), 0) AS INTEGER))" +
                         " FROM " + m_sThisCollection +
                         " WHERE json_extract(data,'$.userId') = ?")) {
                oPs.setString(1, sUserId);
                try (java.sql.ResultSet oRs = oPs.executeQuery()) {
                    if (oRs.next()) {
                        long lValue = oRs.getLong(1);
                        lStorageUsage = oRs.wasNull() ? 0L : lValue;
                    } else {
                        lStorageUsage = 0L;
                    }
                }
            }

        } catch (Exception oEx) {
            WasdiLog.errorLog("WorkspaceRepository.getStorageUsageForUser. Error: ", oEx);
        }

        return lStorageUsage;

    }
}
