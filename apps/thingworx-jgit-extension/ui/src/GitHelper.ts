// Class to be used at runtime

@TWWidgetDefinition
class GitHelper extends TypescriptWidget {

  @property(bindingTarget, didBind("updateTabVisibility")) selectedRepo!: STRING;
  @property(bindingTarget) commitResult!: boolean;
  @property(bindingTarget) statusResult!: boolean;
  @property(bindingTarget) fetchResult!: boolean;
  @property(bindingTarget) pullResult!: boolean;
  @property(bindingTarget) pushResult!: boolean;
  @property(bindingTarget) branchCreateResult!: boolean;
  @property(bindingTarget) branchSwitchResult!: boolean;
  @property(bindingTarget) branchSwitchResult1!: boolean;
  @property(bindingTarget) branchDeleteResult!: boolean;
  @property(bindingTarget) noRowForBranchSwitchSelected!: boolean;
  @property(bindingTarget) gpgKeyCreateResult!: boolean;
  @property(bindingTarget) gpgKeyUpdateResult!: boolean;
  @property(bindingTarget) gpgKeyDeleteResult!: boolean;
  @property(bindingTarget) gpgKeyVerifyResult!: boolean;
  @property(bindingTarget) gpgSetSigningeResult!: boolean;
  @property(bindingTarget) createRepositoryResult!: boolean;
  @property(bindingTarget) deleteRepositoryResult!: boolean;
  @property(bindingTarget) pushError!: boolean;
  @property(bindingSource) tabsVisible: boolean = true;
  @property(bindingSource) selectMessageVisible: boolean = false;
  @property(bindingSource) selectedFile!: string;
  @property(bindingSource) commitButtonDisabled: boolean = true;
  @property(bindingSource) pullIconVisbile: boolean = false;
  @property(bindingSource) pullButtonVisible: boolean = true;
  @property(bindingSource) createBranchProgressIconVisbile: boolean = false;
  @property(bindingSource) createBranchButtonDisabled: boolean = true;
  @property(bindingTarget, didBind("updateCommitButton")) commitMessage!: STRING;
  @property(bindingTarget, didBind("updateCreateBranchButton")) newBranchName!: STRING;
  @property(bindingTarget, didBind("updateCreateBranchButton")) sourceBranch!: STRING;

  @twevent addError!: TWEvent;
  @twevent commitError!: TWEvent;
  @twevent commitSuccess!: TWEvent;
  @twevent pullFailure!: TWEvent;
  @twevent pullSuccess!: TWEvent;
  @twevent pushFailure!: TWEvent;
  @twevent pushSuccess!: TWEvent;
  @twevent checkoutNewBranch!: TWEvent;
  @twevent branchOperationSuccess!: TWEvent;
  @twevent branchOperationFailure!: TWEvent;
  @twevent gpgKeyOperationSuccess!: TWEvent;
  @twevent gpgKeyOperationFailure!: TWEvent;
  @twevent createRepositorySuccess!: TWEvent;
  @twevent createRepositoryFailure!: TWEvent;
  @twevent deleteRepositorySuccess!: TWEvent;
  @twevent deleteRepositoryFailure!: TWEvent;

  private checkoutNewBranchSelected: boolean = false;

  private updateTabVisibility() {
    this.tabsVisible = (this.selectedRepo !== "" || typeof this.selectedRepo !== "undefined");
    this.selectMessageVisible = !this.tabsVisible;
  }

  @service showBranchCreateIcon() {
    this.createBranchProgressIconVisbile = true;
  }

  @service statusCompleted() {
    console.log("GitHelper.statusCompleted(): this.statusResult = " + this.statusResult);
  }

  @service fetchCompleted() {
    console.log("GitHelper.fetchCompleted(): this.fetchResult = " + this.fetchResult);
  }

  @service updateCommitButton() {
    this.commitButtonDisabled = !this.commitMessage || !this.commitMessage.trim();
  }

  @service updateCreateBranchButton() {
    console.log("GitHelper.updateCreateBranchButton(): this.newBranchName = " + this.newBranchName +
      ", this.sourceBranch = " + this.sourceBranch);
    this.createBranchButtonDisabled = !this.newBranchName;
  }

  @service startPull() {
    this.pullButtonVisible = false;
    this.pullIconVisbile = true;
  }

  @service createRepositoryCompleted() {
    console.log("GitHelper.createRepositoryCompleted(): createRepositoryResult = " + this.createRepositoryResult);
    if (this.createRepositoryResult) {
      this.createRepositoryFailure();
    } else {
      this.createRepositorySuccess();
    }
  }

  @service deleteRepositoryCompleted() {
    console.log("GitHelper.deleteRepositoryCompleted(): deleteRepositoryResult = " + this.deleteRepositoryResult);
    if (this.deleteRepositoryResult) {
      this.deleteRepositoryFailure();
    } else {
      this.deleteRepositorySuccess();
    }
  }

  @service commitCompleted() {
    console.log("GitHelper.commitCompleted(): commitResult = " + this.commitResult);
    if (this.commitResult) {
      this.commitError();
    } else {
      this.commitSuccess();
    }
  }

  @service pushCompleted() {
    console.log("GitHelper.pushCompleted(): pushResult = " + this.pushResult);
    if (this.pushResult) {
      this.pushFailure();
    } else {
      this.pushSuccess();
    }
  }

  @service pullCompleted() {
    console.log("GitHelper.pullCompleted(): pullResult = " + this.pullResult);
    if (this.pullResult) {
      this.pullFailure();
    } else {
      this.pullSuccess();
    }
    this.pullButtonVisible = true;
    this.pullIconVisbile = false;
  }


  @service createNewBranchCompleted() {
    console.log("GitHelper.createNewBranchCompleted(): branchCreateResult = " + this.branchCreateResult);
    if (this.branchCreateResult) {
      this.branchOperationFailure();
    } else if (this.checkoutNewBranchSelected) {
      this.checkoutNewBranchSelected = false;
      this.checkoutNewBranch();
    } else {
      this.branchOperationSuccess();
    }
    this.createBranchProgressIconVisbile = false;
    this.createBranchButtonDisabled = true;
  }

  @service deleteBranchCompleted() {
    console.log("GitHelper.deleteBranchCompleted(): branchDeleteResult = " + this.branchDeleteResult);
    if (this.branchDeleteResult) {
      this.branchOperationFailure();
    } else {
      this.branchOperationSuccess();
    }
    this.createBranchButtonDisabled = true;
  }

  @service switchBranchCompleted() {
    console.log("GitHelper.switchBranchCompleted(): branchSwitchResult = " + this.branchSwitchResult);
    if (this.branchSwitchResult) {
      this.branchOperationFailure();
    } else {
      this.branchOperationSuccess();
    }
  }
  @service switchBranch1Completed() {
    console.log("GitHelper.switchBranc1hCompleted(): branchSwitchResult1 = " + this.branchSwitchResult1);
    if (this.branchSwitchResult1) {
      this.branchOperationFailure();
    } else {
      this.branchOperationSuccess();
    }
  }

  @service gpgKeyCreateCompleted() {
    console.log("GitHelper.gpgKeyCreateCompleted(): gpgKeyCreateResult = " + this.gpgKeyCreateResult);
    if (this.gpgKeyCreateResult) {
      this.gpgKeyOperationFailure();
    } else {
      this.gpgKeyOperationSuccess();
    }
  }

  @service gpgKeyUpdateCompleted() {
    console.log("GitHelper.gpgKeyUpdateCompleted(): gpgKeyUpdateResult = " + this.gpgKeyUpdateResult);
    if (this.gpgKeyUpdateResult) {
      this.gpgKeyOperationFailure();
    } else {
      this.gpgKeyOperationSuccess();
    }
  }

  @service gpgKeyDeleteCompleted() {
    console.log("GitHelper.gpgKeyDeleteCompleted(): gpgKeyDeleteResult = " + this.gpgKeyDeleteResult);
    if (this.gpgKeyDeleteResult) {
      this.gpgKeyOperationFailure();
    } else {
      this.gpgKeyOperationSuccess();
    }
  }

  @service gpgKeyVerifyCompleted() {
    console.log("GitHelper.gpgKeyVerifyCompleted(): gpgKeyVerifyResult = " + this.gpgKeyVerifyResult);
    if (this.gpgKeyVerifyResult) {
      this.gpgKeyOperationFailure();
    } else {
      this.gpgKeyOperationSuccess();
    }
  }

  @service gpgKeySetSigningCompleted() {
    console.log("GitHelper.gpgKeySetSigningCompleted(): gpgSetSigningeResult = " + this.gpgSetSigningeResult);
    if (this.gpgSetSigningeResult) {
      this.gpgKeyOperationFailure();
    } else {
      this.gpgKeyOperationSuccess();
    }
  }

  @service setCheckoutNewBranchToTrue() {
    this.checkoutNewBranchSelected = true;
  }

  beforeDestroy() {

  }
}
