class Wasdi {
  constructor() {
    console.log("Library constructor loaded");
  }

  myMethod = (): boolean => {
    console.log("Library method fired");
    return true;
  };
}

let wasdi = new Wasdi();
export default wasdi;
