

export class WorkspaceEditorViewModel {
    private workspaceId!: string;
    private name!: string;
    private userId!: string;
    private apiUrl!: string;
    private creationDate!: Date;
    private lastEditDate!: Date;
    private sharedUsers!: string[];
    private nodeCode!: string;
    private activeNode!: boolean;
    private processesCount!: number;
    private cloudProvider!: string;
    private slaLink!: string;
    private isPublic: boolean = false;
    private readOnly: boolean = false;

    constructor(){}
}