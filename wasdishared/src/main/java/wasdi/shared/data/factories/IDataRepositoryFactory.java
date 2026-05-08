package wasdi.shared.data.factories;

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
 * Centralized factory contract for repository backends.
 */
public interface IDataRepositoryFactory {

    IAppPaymentRepositoryBackend createAppPaymentRepository();

    IAppsCategoriesRepositoryBackend createAppsCategoriesRepository();

    ICloudProviderRepositoryBackend createCloudProviderRepository();

    ICommentRepositoryBackend createCommentRepository();

    ICounterRepositoryBackend createCounterRepository();

    ICreditsPagackageRepositoryBackend createCreditsPagackageRepository();

    IDownloadedFilesRepositoryBackend createDownloadedFilesRepository();

    IJupyterNotebookRepositoryBackend createJupyterNotebookRepository();

    IMetricsEntryRepositoryBackend createMetricsEntryRepository();

    INodeRepositoryBackend createNodeRepository();

    IOgcProcessesTaskRepositoryBackend createOgcProcessesTaskRepository();

    IOpenEOJobRepositoryBackend createOpenEOJobRepository();

    IOrganizationRepositoryBackend createOrganizationRepository();

    IParametersRepositoryBackend createParametersRepository();

    IProcessorLogRepositoryBackend createProcessorLogRepository();

    IProcessorParametersTemplateRepositoryBackend createProcessorParametersTemplateRepository();

    IProcessorRepositoryBackend createProcessorRepository();

    IProcessorUIRepositoryBackend createProcessorUIRepository();

    IProcessWorkspaceRepositoryBackend createProcessWorkspaceRepository();

    IProductWorkspaceRepositoryBackend createProductWorkspaceRepository();

    IProjectRepositoryBackend createProjectRepository();

    IPublishedBandsRepositoryBackend createPublishedBandsRepository();

    IReviewRepositoryBackend createReviewRepository();

    IS3VolumeRepositoryBackend createS3VolumeRepository();

    IScheduleRepositoryBackend createScheduleRepository();

    ISessionRepositoryBackend createSessionRepository();

    ISnapWorkflowRepositoryBackend createSnapWorkflowRepository();

    IStyleRepositoryBackend createStyleRepository();

    ISubscriptionRepositoryBackend createSubscriptionRepository();

    IUserRepositoryBackend createUserRepository();

    IUserResourcePermissionRepositoryBackend createUserResourcePermissionRepository();

    IWorkspaceRepositoryBackend createWorkspaceRepository();

}
