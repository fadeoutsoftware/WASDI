package wasdi.shared.data.sqlite;

import wasdi.shared.data.factories.IDataRepositoryFactory;
import wasdi.shared.data.interfaces.IAppPaymentRepositoryBackend;
import wasdi.shared.data.interfaces.IAppsCategoriesRepositoryBackend;
import wasdi.shared.data.interfaces.ICloudProviderRepositoryBackend;
import wasdi.shared.data.interfaces.ICommentRepositoryBackend;
import wasdi.shared.data.interfaces.ICounterRepositoryBackend;
import wasdi.shared.data.interfaces.ICreditsPagackageRepositoryBackend;
import wasdi.shared.data.interfaces.IDownloadedFilesRepositoryBackend;
import wasdi.shared.data.interfaces.IJupyterNotebookRepositoryBackend;
import wasdi.shared.data.interfaces.IMetricsEntryRepositoryBackend;
import wasdi.shared.data.interfaces.INodeRepositoryBackend;
import wasdi.shared.data.interfaces.IOgcProcessesTaskRepositoryBackend;
import wasdi.shared.data.interfaces.IOpenEOJobRepositoryBackend;
import wasdi.shared.data.interfaces.IOrganizationRepositoryBackend;
import wasdi.shared.data.interfaces.IParametersRepositoryBackend;
import wasdi.shared.data.interfaces.IProcessWorkspaceRepositoryBackend;
import wasdi.shared.data.interfaces.IProcessorLogRepositoryBackend;
import wasdi.shared.data.interfaces.IProcessorParametersTemplateRepositoryBackend;
import wasdi.shared.data.interfaces.IProcessorRepositoryBackend;
import wasdi.shared.data.interfaces.IProcessorUIRepositoryBackend;
import wasdi.shared.data.interfaces.IProductWorkspaceRepositoryBackend;
import wasdi.shared.data.interfaces.IProjectRepositoryBackend;
import wasdi.shared.data.interfaces.IPublishedBandsRepositoryBackend;
import wasdi.shared.data.interfaces.IReviewRepositoryBackend;
import wasdi.shared.data.interfaces.IS3VolumeRepositoryBackend;
import wasdi.shared.data.interfaces.IScheduleRepositoryBackend;
import wasdi.shared.data.interfaces.ISessionRepositoryBackend;
import wasdi.shared.data.interfaces.ISnapWorkflowRepositoryBackend;
import wasdi.shared.data.interfaces.IStyleRepositoryBackend;
import wasdi.shared.data.interfaces.ISubscriptionRepositoryBackend;
import wasdi.shared.data.interfaces.IUserRepositoryBackend;
import wasdi.shared.data.interfaces.IUserResourcePermissionRepositoryBackend;
import wasdi.shared.data.interfaces.IWorkspaceRepositoryBackend;

/**
 * Mongo implementation of centralized repository backend factory.
 */
public class SqliteDataRepositoryFactory implements IDataRepositoryFactory {

    @Override
    public IAppPaymentRepositoryBackend createAppPaymentRepository() {
        IAppPaymentRepositoryBackend oRepositoryBackend = new SqliteAppPaymentRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IAppsCategoriesRepositoryBackend createAppsCategoriesRepository() {
        IAppsCategoriesRepositoryBackend oRepositoryBackend = new SqliteAppsCategoriesRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public ICloudProviderRepositoryBackend createCloudProviderRepository() {
        ICloudProviderRepositoryBackend oRepositoryBackend = new SqliteCloudProviderRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public ICommentRepositoryBackend createCommentRepository() {
        ICommentRepositoryBackend oRepositoryBackend = new SqliteCommentRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public ICounterRepositoryBackend createCounterRepository() {
        ICounterRepositoryBackend oRepositoryBackend = new SqliteCounterRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public ICreditsPagackageRepositoryBackend createCreditsPagackageRepository() {
        ICreditsPagackageRepositoryBackend oRepositoryBackend = new SqliteCreditsPagackageRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IDownloadedFilesRepositoryBackend createDownloadedFilesRepository() {
        IDownloadedFilesRepositoryBackend oRepositoryBackend = new SqliteDownloadedFilesRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IJupyterNotebookRepositoryBackend createJupyterNotebookRepository() {
        IJupyterNotebookRepositoryBackend oRepositoryBackend = new SqliteJupyterNotebookRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IMetricsEntryRepositoryBackend createMetricsEntryRepository() {
        IMetricsEntryRepositoryBackend oRepositoryBackend = new SqliteMetricsEntryRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public INodeRepositoryBackend createNodeRepository() {
        INodeRepositoryBackend oRepositoryBackend = new SqliteNodeRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IOgcProcessesTaskRepositoryBackend createOgcProcessesTaskRepository() {
        IOgcProcessesTaskRepositoryBackend oRepositoryBackend = new SqliteOgcProcessesTaskRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IOpenEOJobRepositoryBackend createOpenEOJobRepository() {
        IOpenEOJobRepositoryBackend oRepositoryBackend = new SqliteOpenEOJobRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IOrganizationRepositoryBackend createOrganizationRepository() {
        IOrganizationRepositoryBackend oRepositoryBackend = new SqliteOrganizationRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IParametersRepositoryBackend createParametersRepository() {
        IParametersRepositoryBackend oRepositoryBackend = new SqliteParametersRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IProcessorLogRepositoryBackend createProcessorLogRepository() {
        IProcessorLogRepositoryBackend oRepositoryBackend = new SqliteProcessorLogRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IProcessorParametersTemplateRepositoryBackend createProcessorParametersTemplateRepository() {
        IProcessorParametersTemplateRepositoryBackend oRepositoryBackend = new SqliteProcessorParametersTemplateRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IProcessorRepositoryBackend createProcessorRepository() {
        IProcessorRepositoryBackend oRepositoryBackend = new SqliteProcessorRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IProcessorUIRepositoryBackend createProcessorUIRepository() {
        IProcessorUIRepositoryBackend oRepositoryBackend = new SqliteProcessorUIRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IProcessWorkspaceRepositoryBackend createProcessWorkspaceRepository() {
        IProcessWorkspaceRepositoryBackend oRepositoryBackend = new SqliteProcessWorkspaceRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IProductWorkspaceRepositoryBackend createProductWorkspaceRepository() {
        IProductWorkspaceRepositoryBackend oRepositoryBackend = new SqliteProductWorkspaceRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IProjectRepositoryBackend createProjectRepository() {
        IProjectRepositoryBackend oRepositoryBackend = new SqliteProjectRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IPublishedBandsRepositoryBackend createPublishedBandsRepository() {
        IPublishedBandsRepositoryBackend oRepositoryBackend = new SqlitePublishedBandsRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IReviewRepositoryBackend createReviewRepository() {
        IReviewRepositoryBackend oRepositoryBackend = new SqliteReviewRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IS3VolumeRepositoryBackend createS3VolumeRepository() {
        IS3VolumeRepositoryBackend oRepositoryBackend = new SqliteS3VolumeRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IScheduleRepositoryBackend createScheduleRepository() {
        IScheduleRepositoryBackend oRepositoryBackend = new SqliteScheduleRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public ISessionRepositoryBackend createSessionRepository() {
        ISessionRepositoryBackend oRepositoryBackend = new SqliteSessionRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public ISnapWorkflowRepositoryBackend createSnapWorkflowRepository() {
        ISnapWorkflowRepositoryBackend oRepositoryBackend = new SqliteSnapWorkflowRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IStyleRepositoryBackend createStyleRepository() {
        IStyleRepositoryBackend oRepositoryBackend = new SqliteStyleRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public ISubscriptionRepositoryBackend createSubscriptionRepository() {
        ISubscriptionRepositoryBackend oRepositoryBackend = new SqliteSubscriptionRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IUserRepositoryBackend createUserRepository() {
        IUserRepositoryBackend oRepositoryBackend = new SqliteUserRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IUserResourcePermissionRepositoryBackend createUserResourcePermissionRepository() {
        IUserResourcePermissionRepositoryBackend oRepositoryBackend = new SqliteUserResourcePermissionRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IWorkspaceRepositoryBackend createWorkspaceRepository() {
        IWorkspaceRepositoryBackend oRepositoryBackend = new SqliteWorkspaceRepositoryBackend();
        return oRepositoryBackend;
    }

}
