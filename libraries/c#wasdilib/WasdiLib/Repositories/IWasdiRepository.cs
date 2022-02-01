using WasdiLib.Models;

namespace WasdiLib.Repositories
{
    internal interface IWasdiRepository
    {

        Task<WasdiResponse> HelloWasdi();

    }
}