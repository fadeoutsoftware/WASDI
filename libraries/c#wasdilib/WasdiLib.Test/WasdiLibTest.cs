using NUnit.Framework;

namespace WasdiLib.Test
{
    [TestFixture]
    public class WasdiLibTest
    {

        private WasdiLib wasdi;

        [SetUp]
        public void Setup()
        {
            wasdi = new WasdiLib();
        }

        [Test]
        public void Test_0_HelloWasdi_WithoutCredentials_ShouldSayHelloWasdi()
        {
            string expected = "Hello Wasdi!!";
            string actual = wasdi.HelloWasdi();

            Assert.AreEqual(expected, actual);
            Assert.That(actual, Is.EqualTo(expected));
        }
    }
}
