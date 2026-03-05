package wasdi.shared.data.no2;

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
import wasdi.shared.data.mongo.MongoDataRepositoryFactory;

/**
 * Transitional NO2 repository factory.
 *
 * CounterRepository is routed to the first NO2 backend. Other repositories
 * currently fallback to Mongo factory until each backend is ported.
 */
public class No2DataRepositoryFactory implements IDataRepositoryFactory {

	private final IDataRepositoryFactory m_oMongoFactory;

	public No2DataRepositoryFactory() {
		m_oMongoFactory = new MongoDataRepositoryFactory();
	}

	@Override
	public IAppPaymentRepositoryBackend createAppPaymentRepository() {
		return new No2AppPaymentRepositoryBackend();
	}

	@Override
	public IAppsCategoriesRepositoryBackend createAppsCategoriesRepository() {
		return new No2AppsCategoriesRepositoryBackend();
	}

	@Override
	public ICloudProviderRepositoryBackend createCloudProviderRepository() {
		return new No2CloudProviderRepositoryBackend();
	}

	@Override
	public ICommentRepositoryBackend createCommentRepository() {
		return new No2CommentRepositoryBackend();
	}

	@Override
	public ICounterRepositoryBackend createCounterRepository() {
		return new No2CounterRepositoryBackend();
	}

	@Override
	public ICreditsPagackageRepositoryBackend createCreditsPagackageRepository() {
		return new No2CreditsPagackageRepositoryBackend();
	}

	@Override
	public IDownloadedFilesRepositoryBackend createDownloadedFilesRepository() {
		return new No2DownloadedFilesRepositoryBackend();
	}

	@Override
	public IJupyterNotebookRepositoryBackend createJupyterNotebookRepository() {
		return new No2JupyterNotebookRepositoryBackend();
	}

	@Override
	public IMetricsEntryRepositoryBackend createMetricsEntryRepository() {
		return new No2MetricsEntryRepositoryBackend();
	}

	@Override
	public INodeRepositoryBackend createNodeRepository() {
		return new No2NodeRepositoryBackend();
	}

	@Override
	public IOgcProcessesTaskRepositoryBackend createOgcProcessesTaskRepository() {
		return new No2OgcProcessesTaskRepositoryBackend();
	}

	@Override
	public IOpenEOJobRepositoryBackend createOpenEOJobRepository() {
		return new No2OpenEOJobRepositoryBackend();
	}

	@Override
	public IOrganizationRepositoryBackend createOrganizationRepository() {
		return new No2OrganizationRepositoryBackend();
	}

	@Override
	public IParametersRepositoryBackend createParametersRepository() {
		return new No2ParametersRepositoryBackend();
	}

	@Override
	public IProcessorLogRepositoryBackend createProcessorLogRepository() {
		return new No2ProcessorLogRepositoryBackend();
	}

	@Override
	public IProcessorParametersTemplateRepositoryBackend createProcessorParametersTemplateRepository() {
		return new No2ProcessorParametersTemplateRepositoryBackend();
	}

	@Override
	public IProcessorRepositoryBackend createProcessorRepository() {
		return new No2ProcessorRepositoryBackend();
	}

	@Override
	public IProcessorUIRepositoryBackend createProcessorUIRepository() {
		return new No2ProcessorUIRepositoryBackend();
	}

	@Override
	public IProcessWorkspaceRepositoryBackend createProcessWorkspaceRepository() {
		return new No2ProcessWorkspaceRepositoryBackend();
	}

	@Override
	public IProductWorkspaceRepositoryBackend createProductWorkspaceRepository() {
		return new No2ProductWorkspaceRepositoryBackend();
	}

	@Override
	public IProjectRepositoryBackend createProjectRepository() {
		return new No2ProjectRepositoryBackend();
	}

	@Override
	public IPublishedBandsRepositoryBackend createPublishedBandsRepository() {
		return new No2PublishedBandsRepositoryBackend();
	}

	@Override
	public IReviewRepositoryBackend createReviewRepository() {
		return new No2ReviewRepositoryBackend();
	}

	@Override
	public IS3VolumeRepositoryBackend createS3VolumeRepository() {
		return new No2S3VolumeRepositoryBackend();
	}

	@Override
	public IScheduleRepositoryBackend createScheduleRepository() {
		return new No2ScheduleRepositoryBackend();
	}

	@Override
	public ISessionRepositoryBackend createSessionRepository() {
		return new No2SessionRepositoryBackend();
	}

	@Override
	public ISnapWorkflowRepositoryBackend createSnapWorkflowRepository() {
		return new No2SnapWorkflowRepositoryBackend();
	}

	@Override
	public IStyleRepositoryBackend createStyleRepository() {
		return new No2StyleRepositoryBackend();
	}

	@Override
	public ISubscriptionRepositoryBackend createSubscriptionRepository() {
		return new No2SubscriptionRepositoryBackend();
	}

	@Override
	public IUserRepositoryBackend createUserRepository() {
		return new No2UserRepositoryBackend();
	}

	@Override
	public IUserResourcePermissionRepositoryBackend createUserResourcePermissionRepository() {
		return new No2UserResourcePermissionRepositoryBackend();
	}

	@Override
	public IWorkspaceRepositoryBackend createWorkspaceRepository() {
		return new No2WorkspaceRepositoryBackend();
	}
}
