using System.Collections.Generic;
using System.IO;
using Microsoft.Extensions.Configuration;

using NUnit.Framework;

using WasdiLib.Models;


namespace WasdiLib.Test
{
    [TestFixture]
    public class WasdiLibTest
    {

        private IConfiguration _configuration;

        private WasdiLib wasdi;

        [SetUp]
        public void Setup()
        {

            string path = Path.GetFullPath("appsettings.json");

            _configuration = new ConfigurationBuilder()
                .AddJsonFile(path, optional: false, reloadOnChange: true)
                .Build();

            wasdi = new WasdiLib();
            wasdi.Init();
        }

        [Test]
        public void Test_0_HelloWasdi_WithoutCredentials_ShouldSayHelloWasdi()
        {
            string expected = "Hello Wasdi!!";
            string actual = wasdi.HelloWasdi();

            Assert.AreEqual(expected, actual);
            Assert.That(actual, Is.EqualTo(expected));
        }

        [Test]
        public void Test_81_GetWorkspaceIdByName_And_GettWorkspaceNameById()
        {
            string workspaceNameExpected = "TestWorkspace";
            string workspaceId = wasdi.GetWorkspaceIdByName(workspaceNameExpected);

            Assert.IsNotNull(workspaceId);

            string workspaceNameActual = wasdi.GetWorkspaceNameById(workspaceId);

            Assert.AreEqual(workspaceNameExpected, workspaceNameActual);
        }

        [Test]
        public void Test_82_GetWorkspaceIdByName_And_GetWorkspaceUrlByWsId()
        {
            string workspaceName = "TestWorkspace";
            string workspaceId = wasdi.GetWorkspaceIdByName(workspaceName);

            Assert.IsNotNull(workspaceId);

            // "https://test.wasdi.net/wasdiwebserver/rest/ws/getws?workspace=" + workspaceId;
            string workspaceUrlExpected = null;
            string workspaceUrlActual = wasdi.GetWorkspaceUrlByWsId(workspaceId);

            //Assert.AreEqual(workspaceUrlExpected, workspaceUrlActual);
            Assert.IsNull(workspaceUrlActual);
        }

        [Test]
        public void Test_84_GetWorkspaces()
        {
            string userIdExpected = _configuration!["USER"]!;

            List<Workspace> workspaces = wasdi.GetWorkspaces();

            Assert.IsNotNull(workspaces);

            foreach (Workspace workspace in workspaces)
            {
                Assert.AreEqual(userIdExpected, (string)workspace.OwnerUserId);
            }
        }

        [Test]
        [Ignore("Ignore this test as it is not generic")]
        public void Test_66_downloadFile()
        {
            string workspaceName = "ERA5_WORKSPACE_TEST";
            string workspaceId = "8474061b-3c7c-4f72-9289-4ff9a67a8432";

            string foundWorkspaceId = wasdi.GetWorkspaceIdByName(workspaceName);
            Assert.AreEqual(workspaceId, foundWorkspaceId);

            string openedWorkspaceId = wasdi.OpenWorkspace(workspaceName);
            Assert.AreEqual(foundWorkspaceId, openedWorkspaceId);

            string activeWorkspaceId = wasdi.GetActiveWorkspace();
            Assert.AreEqual(foundWorkspaceId, activeWorkspaceId);

            string sFileName = "RIVER-FLDglobal-composite1_20220111_000000_part071.tif";
//            string sFileName = "S1A_IW_GRDH_1SDV_20220102T172430_20220102T172455_041285_04E83C_1902.zip";

            string sFullPath = wasdi.GetPath(sFileName);

            wasdi.WasdiLog("sFullPath: " + sFullPath);

//            string expectedFullPath = "C:/temp/wasdi/petru.petrescu@wasdi.cloud\\" + workspaceId + "\\" + sFileName;
            string expectedFullPath = "C:\\Users\\PetruPetrescu/.wasdi/petru.petrescu@wasdi.cloud\\" + workspaceId + "\\" + sFileName;

            Assert.AreEqual(expectedFullPath, sFullPath);
        }


        [Test]
        public void Test_82_GetWorkspaceIdByName_And_GettWorkspaceOwnerByName_And_GettWorkspaceOwnerById()
        {
            string workspaceNameExpected = "TestWorkspace";
            string workspaceId = wasdi.GetWorkspaceIdByName(workspaceNameExpected);
            string workspacesOwnerUserIdByName = wasdi.GetWorkspaceOwnerByName(workspaceNameExpected);
            string workspacesOwnerUserIdById = wasdi.GetWorkspaceOwnerByWSId(workspaceId);

            Assert.IsNotNull(workspaceId);
            Assert.IsNotNull(workspacesOwnerUserIdByName);
            Assert.IsNotNull(workspacesOwnerUserIdById);

            Assert.AreEqual(workspacesOwnerUserIdById, workspacesOwnerUserIdByName);
        }

        [Test]
        public void Test_80_CreateWorkspace()
        {
            string workspaceNameExpected = "TestWorkspace";
            string workspaceIdActual = wasdi.CreateWorkspace(workspaceNameExpected);

            Assert.IsNotNull(workspaceIdActual);
            string workspaceNameActual = wasdi.GetWorkspaceNameById(workspaceIdActual);

            Assert.AreEqual(workspaceNameExpected, workspaceNameActual);
        }

        [Test]
        public void Test_90_DeleteWorkspace()
        {
            string workspaceName = "TestWorkspace";
            string workspaceId = wasdi.GetWorkspaceIdByName(workspaceName);

            if (!string.IsNullOrEmpty(workspaceId))
            {
                string outcome = wasdi.DeleteWorkspace(workspaceId);

                Assert.IsEmpty(outcome);
                workspaceName = wasdi.GetWorkspaceNameById(workspaceId);

                Assert.IsEmpty(workspaceName);
            }
        }

        [Test]
        public void Test_85_GetProcessWorkspacesByWorkspaceId()
        {
            string userIdExpected = _configuration!["USER"]!;

            string workspaceName = "ERA5_WORKSPACE_TEST";
            string workspaceId = wasdi.GetWorkspaceIdByName(workspaceName);
            Assert.IsNotNull(workspaceId);

            wasdi.OpenWorkspace(workspaceName);

            List<ProcessWorkspace> processWorkspaces = wasdi.GetProcessWorkspacesByWorkspaceId(workspaceId);

            Assert.IsNotNull(processWorkspaces);

            foreach (ProcessWorkspace processWorkspace in processWorkspaces)
            {
                Assert.AreEqual(userIdExpected, processWorkspace.UserId);
            }
        }

        [Test]
        public void Test_84_GetWorkflows()
        {
            string userIdExpected = _configuration!["USER"]!;

            List<Workflow> workflows = wasdi.GetWorkflows();

            Assert.IsNotNull(workflows);

            //foreach (Workflow workflow in workflows)
            //{
            //    Assert.AreEqual(userIdExpected, workflow.UserId);
            //}
        }
    }
}
