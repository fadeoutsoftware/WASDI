export class WorkspaceListInfoViewModel {
    workspaceId!: string;
    workspaceName!: string;
    ownerUserId!: string;
    sharedUsers: string[] = [];
    activeNode!: boolean;
    nodeCode!: string;
    creationDate!: Date;
    isPublic: boolean = false;
    readOnly: boolean = false;
}