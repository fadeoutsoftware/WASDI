package wasdi.shared.data.mongo;

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
public class MongoDataRepositoryFactory implements IDataRepositoryFactory {

    @Override
    public IAppPaymentRepositoryBackend createAppPaymentRepository() {
        IAppPaymentRepositoryBackend oRepositoryBackend = new MongoAppPaymentRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IAppsCategoriesRepositoryBackend createAppsCategoriesRepository() {
        IAppsCategoriesRepositoryBackend oRepositoryBackend = new MongoAppsCategoriesRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public ICloudProviderRepositoryBackend createCloudProviderRepository() {
        ICloudProviderRepositoryBackend oRepositoryBackend = new MongoCloudProviderRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public ICommentRepositoryBackend createCommentRepository() {
        ICommentRepositoryBackend oRepositoryBackend = new MongoCommentRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public ICounterRepositoryBackend createCounterRepository() {
        ICounterRepositoryBackend oRepositoryBackend = new MongoCounterRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public ICreditsPagackageRepositoryBackend createCreditsPagackageRepository() {
        ICreditsPagackageRepositoryBackend oRepositoryBackend = new MongoCreditsPagackageRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IDownloadedFilesRepositoryBackend createDownloadedFilesRepository() {
        IDownloadedFilesRepositoryBackend oRepositoryBackend = new MongoDownloadedFilesRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IJupyterNotebookRepositoryBackend createJupyterNotebookRepository() {
        IJupyterNotebookRepositoryBackend oRepositoryBackend = new MongoJupyterNotebookRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IMetricsEntryRepositoryBackend createMetricsEntryRepository() {
        IMetricsEntryRepositoryBackend oRepositoryBackend = new MongoMetricsEntryRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public INodeRepositoryBackend createNodeRepository() {
        INodeRepositoryBackend oRepositoryBackend = new MongoNodeRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IOgcProcessesTaskRepositoryBackend createOgcProcessesTaskRepository() {
        IOgcProcessesTaskRepositoryBackend oRepositoryBackend = new MongoOgcProcessesTaskRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IOpenEOJobRepositoryBackend createOpenEOJobRepository() {
        IOpenEOJobRepositoryBackend oRepositoryBackend = new MongoOpenEOJobRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IOrganizationRepositoryBackend createOrganizationRepository() {
        IOrganizationRepositoryBackend oRepositoryBackend = new MongoOrganizationRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IParametersRepositoryBackend createParametersRepository() {
        IParametersRepositoryBackend oRepositoryBackend = new MongoParametersRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IProcessorLogRepositoryBackend createProcessorLogRepository() {
        IProcessorLogRepositoryBackend oRepositoryBackend = new MongoProcessorLogRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IProcessorParametersTemplateRepositoryBackend createProcessorParametersTemplateRepository() {
        IProcessorParametersTemplateRepositoryBackend oRepositoryBackend = new MongoProcessorParametersTemplateRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IProcessorRepositoryBackend createProcessorRepository() {
        IProcessorRepositoryBackend oRepositoryBackend = new MongoProcessorRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IProcessorUIRepositoryBackend createProcessorUIRepository() {
        IProcessorUIRepositoryBackend oRepositoryBackend = new MongoProcessorUIRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IProcessWorkspaceRepositoryBackend createProcessWorkspaceRepository() {
        IProcessWorkspaceRepositoryBackend oRepositoryBackend = new MongoProcessWorkspaceRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IProductWorkspaceRepositoryBackend createProductWorkspaceRepository() {
        IProductWorkspaceRepositoryBackend oRepositoryBackend = new MongoProductWorkspaceRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IProjectRepositoryBackend createProjectRepository() {
        IProjectRepositoryBackend oRepositoryBackend = new MongoProjectRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IPublishedBandsRepositoryBackend createPublishedBandsRepository() {
        IPublishedBandsRepositoryBackend oRepositoryBackend = new MongoPublishedBandsRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IReviewRepositoryBackend createReviewRepository() {
        IReviewRepositoryBackend oRepositoryBackend = new MongoReviewRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IS3VolumeRepositoryBackend createS3VolumeRepository() {
        IS3VolumeRepositoryBackend oRepositoryBackend = new MongoS3VolumeRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IScheduleRepositoryBackend createScheduleRepository() {
        IScheduleRepositoryBackend oRepositoryBackend = new MongoScheduleRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public ISessionRepositoryBackend createSessionRepository() {
        ISessionRepositoryBackend oRepositoryBackend = new MongoSessionRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public ISnapWorkflowRepositoryBackend createSnapWorkflowRepository() {
        ISnapWorkflowRepositoryBackend oRepositoryBackend = new MongoSnapWorkflowRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IStyleRepositoryBackend createStyleRepository() {
        IStyleRepositoryBackend oRepositoryBackend = new MongoStyleRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public ISubscriptionRepositoryBackend createSubscriptionRepository() {
        ISubscriptionRepositoryBackend oRepositoryBackend = new MongoSubscriptionRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IUserRepositoryBackend createUserRepository() {
        IUserRepositoryBackend oRepositoryBackend = new MongoUserRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IUserResourcePermissionRepositoryBackend createUserResourcePermissionRepository() {
        IUserResourcePermissionRepositoryBackend oRepositoryBackend = new MongoUserResourcePermissionRepositoryBackend();
        return oRepositoryBackend;
    }

    @Override
    public IWorkspaceRepositoryBackend createWorkspaceRepository() {
        IWorkspaceRepositoryBackend oRepositoryBackend = new MongoWorkspaceRepositoryBackend();
        return oRepositoryBackend;
    }

}
