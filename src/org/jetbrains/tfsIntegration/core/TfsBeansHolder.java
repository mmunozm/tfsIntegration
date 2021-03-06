package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.microsoft.schemas.teamfoundation._2005._06.services.groupsecurity._03.GroupSecurityServiceStub;
import com.microsoft.schemas.teamfoundation._2005._06.services.registration._03.*;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.RepositoryStub;
import com.microsoft.schemas.teamfoundation._2005._06.workitemtracking.clientservices._03.ClientService2Stub;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.configuration.Credentials;
import org.jetbrains.tfsIntegration.core.tfs.TfsUtil;
import org.jetbrains.tfsIntegration.exceptions.HostNotApplicableException;
import org.jetbrains.tfsIntegration.webservice.WebServiceHelper;

import java.net.URI;
import java.rmi.RemoteException;

public class TfsBeansHolder {

  private static final Logger LOG = Logger.getInstance(TfsBeansHolder.class.getName());

  private final URI myServerUri;

  private RepositoryStub myRepository;
  // Newer version of repository service. Required for several operations with local workspaces.
  private RepositoryStub myRepository4;
  private ClientService2Stub myWorkItemTrackingClientService;
  private GroupSecurityServiceStub myGroupSecurityService;
  private String myDownloadUrl;
  private String myUploadUrl;
  private final HttpClient[] myUploadDownloadClients = new HttpClient[2]; // regular, proxy

  public TfsBeansHolder(URI serverUri) {
    myServerUri = serverUri;
  }

  @NotNull
  public RepositoryStub getRepositoryStub(Credentials credentials, ProgressIndicator pi)
    throws HostNotApplicableException, RemoteException {
    if (myRepository == null) {
      createStubs(credentials, pi);
    }
    WebServiceHelper.setupStub(myRepository, credentials, myServerUri);
    return myRepository;
  }

  @NotNull
  public RepositoryStub getRepository4Stub(Credentials credentials, ProgressIndicator pi)
    throws HostNotApplicableException, RemoteException {
    if (myRepository4 == null) {
      createStubs(credentials, pi);
    }
    WebServiceHelper.setupStub(myRepository4, credentials, myServerUri);
    return myRepository4;
  }

  @NotNull
  public ClientService2Stub getWorkItemServiceStub(Credentials credentials, ProgressIndicator pi)
    throws HostNotApplicableException, RemoteException {
    if (myWorkItemTrackingClientService == null) {
      createStubs(credentials, pi);
    }
    WebServiceHelper.setupStub(myWorkItemTrackingClientService, credentials, myServerUri);
    return myWorkItemTrackingClientService;
  }

  @NotNull
  public GroupSecurityServiceStub getGroupSecurityServiceStub(Credentials credentials, ProgressIndicator pi)
    throws HostNotApplicableException, RemoteException {
    if (myGroupSecurityService == null) {
      createStubs(credentials, pi);
    }
    WebServiceHelper.setupStub(myGroupSecurityService, credentials, myServerUri);
    return myGroupSecurityService;
  }

  @NotNull
  public String getDownloadUrl(Credentials credentials, ProgressIndicator pi) throws HostNotApplicableException, RemoteException {
    if (myDownloadUrl == null) {
      createStubs(credentials, pi);
    }
    return myDownloadUrl;
  }

  @NotNull
  public String getUploadUrl(Credentials credentials, ProgressIndicator pi) throws HostNotApplicableException, RemoteException {
    if (myUploadUrl == null) {
      createStubs(credentials, pi);
    }
    return myUploadUrl;
  }

  private void createStubs(Credentials authorizedCredentials, @Nullable ProgressIndicator pi)
    throws RemoteException, HostNotApplicableException {
    LOG.assertTrue(!ApplicationManager.getApplication().isDispatchThread());

    String piText = pi != null ? pi.getText() : null;

    if (pi != null) {
      pi.setText(TFSBundle.message("loading.services"));
    }

    final ConfigurationContext configContext = WebServiceHelper.getStubConfigurationContext();

    RegistrationStub registrationStub =
      new RegistrationStub(configContext, TfsUtil.appendPath(myServerUri, TFSConstants.REGISTRATION_ASMX));
    WebServiceHelper.setupStub(registrationStub, authorizedCredentials, myServerUri);
    final GetRegistrationEntries getRegistrationEntriesParam = new GetRegistrationEntries();
    ArrayOfFrameworkRegistrationEntry registrationEntries =
      registrationStub.getRegistrationEntries(getRegistrationEntriesParam).getGetRegistrationEntriesResult();

    String isccProvider =
      findServicePath(registrationEntries, TFSConstants.VERSION_CONTROL_ENTRY_TYPE, TFSConstants.ISCC_PROVIDER_SERVICE_NAME);
    if (isccProvider == null) {
      throw new HostNotApplicableException(null);
    }
    String isccProvider4 =
      findServicePath(registrationEntries, TFSConstants.VERSION_CONTROL_ENTRY_TYPE, TFSConstants.ISCC_PROVIDER_4_SERVICE_NAME,
                      TFSConstants.ISCC_PROVIDER_SERVICE_NAME);
    if (isccProvider4 == null) {
      throw new HostNotApplicableException(null);
    }
    String download = findServicePath(registrationEntries, TFSConstants.VERSION_CONTROL_ENTRY_TYPE, TFSConstants.DOWNLOAD_SERVICE_NAME);
    if (download == null) {
      throw new HostNotApplicableException(null);
    }
    String upload = findServicePath(registrationEntries, TFSConstants.VERSION_CONTROL_ENTRY_TYPE, TFSConstants.UPLOAD_SERVICE_NAME);
    if (upload == null) {
      throw new HostNotApplicableException(null);
    }
    String workItemService =
      findServicePath(registrationEntries, TFSConstants.WORK_ITEM_TRACKING_ENTRY_TYPE, TFSConstants.WORKITEM_SERVICE_NAME);
    if (workItemService == null) {
      throw new HostNotApplicableException(null);
    }
    String groupSecurityService =
      findServicePath(registrationEntries, TFSConstants.VSTFS_ENTRY_TYPE, TFSConstants.GROUP_SECURITY_SERVICE_NAME);
    if (groupSecurityService == null) {
      throw new HostNotApplicableException(null);
    }
    doCreateStubs(configContext, isccProvider, isccProvider4, download, upload, workItemService, groupSecurityService);

    if (pi != null) {
      pi.setText(piText);
    }
  }

  private void doCreateStubs(@Nullable ConfigurationContext configContext,
                             String isccProvider,
                             String isccProvider4,
                             String download,
                             String upload,
                             String workItemService,
                             String groupSecurity) {
    myDownloadUrl = download;
    myUploadUrl = upload;
    try {
      if (configContext == null) {
        configContext = WebServiceHelper.getStubConfigurationContext();
      }
      myRepository = new RepositoryStub(configContext, TfsUtil.appendPath(myServerUri, isccProvider));
      myRepository4 = new RepositoryStub(configContext, TfsUtil.appendPath(myServerUri, isccProvider4));
      myWorkItemTrackingClientService =
        new ClientService2Stub(configContext, TfsUtil.appendPath(myServerUri, workItemService));
      myGroupSecurityService =
        new GroupSecurityServiceStub(configContext, TfsUtil.appendPath(myServerUri, groupSecurity));
    }
    catch (Exception e) {
      LOG.error("Failed to initialize web service stub", e);
    }
  }
  
  public HttpClient getUploadDownloadClient(boolean forProxy) {
    int index = forProxy ? 1 : 0;
    if (myUploadDownloadClients[index] == null) {
      HttpConnectionManager connManager = new MultiThreadedHttpConnectionManager();
      myUploadDownloadClients[index] = new HttpClient(connManager);
      HttpClientParams clientParams = new HttpClientParams();
      // Set the default timeout in case we have a connection pool starvation to 30sec
      clientParams.setConnectionManagerTimeout(30000);
      myUploadDownloadClients[index].setParams(clientParams);
    }
    return myUploadDownloadClients[index];
  }

  @Nullable
  private static String findServicePath(ArrayOfFrameworkRegistrationEntry registrationEntries, String entryType, String... interfaceNames) {
    if (registrationEntries == null) {
      return null;
    }
    for (FrameworkRegistrationEntry entry : registrationEntries.getRegistrationEntry()) {
      if (entryType.equals(entry.getType())) {
        RegistrationServiceInterface[] interfaces = entry.getServiceInterfaces().getServiceInterface();
        if (interfaces != null) {
          for (String interfaceName : interfaceNames) {
            for (RegistrationServiceInterface anInterface : interfaces) {
              if (interfaceName.equals(anInterface.getName())) {
                return anInterface.getUrl();
              }
            }
          }
        }
      }
    }
    return null;
  }


}
