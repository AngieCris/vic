/* Copyright 2015 VMware, Inc. All rights reserved. -- VMware Confidential */
package com.vmware.vsphere.client.automation.vm.edit;

import org.testng.annotations.Test;

import com.vmware.client.automation.common.spec.TaskSpec;
import com.vmware.client.automation.common.step.VerifyTaskByUiStep;
import com.vmware.client.automation.workflow.common.WorkflowSpec;
import com.vmware.client.automation.workflow.common.WorkflowStepsSequence;
import com.vmware.client.automation.workflow.explorer.TestBedBridge;
import com.vmware.client.automation.workflow.explorer.TestbedSpecConsumer;
import com.vmware.client.automation.workflow.test.TestWorkflowStepContext;
import com.vmware.vsphere.client.automation.common.step.ClickOkSinglePageDialogStep;
import com.vmware.vsphere.client.automation.common.workflow.NGCTestWorkflow;
import com.vmware.vsphere.client.automation.components.navigator.NGCNavigator;
import com.vmware.vsphere.client.automation.components.navigator.spec.VmLocationSpec;
import com.vmware.vsphere.client.automation.components.navigator.step.VmNavigationStep;
import com.vmware.vsphere.client.automation.provider.commontb.CommonTestBedProvider;
import com.vmware.vsphere.client.automation.srv.common.spec.DatastoreSpec;
import com.vmware.vsphere.client.automation.srv.common.spec.HostSpec;
import com.vmware.vsphere.client.automation.srv.common.spec.SpecFactory;
import com.vmware.vsphere.client.automation.srv.common.spec.VcSpec;
import com.vmware.vsphere.client.automation.srv.common.spec.VmSpec;
import com.vmware.vsphere.client.automation.srv.common.step.CreateVmByApiStep;
import com.vmware.vsphere.client.automation.srv.common.step.VerifyVmExistenceByApiStep;
import com.vmware.vsphere.client.automation.vm.common.VmUtil;
import com.vmware.vsphere.client.automation.vm.common.messages.VmTaskMessages;
import com.vmware.vsphere.client.automation.vm.lib.messages.VmHardwareMessages;
import com.vmware.vsphere.client.automation.vm.edit.spec.VmBootOptionsSpec;
import com.vmware.vsphere.client.automation.vm.edit.step.SetVmBootOptionsStep;
import com.vmware.vsphere.client.automation.vm.lib.step.LaunchEditSettingsStep;
import com.vmware.vsphere.client.automation.vm.lib.ops.model.VmOpsModel.VmPowerState;
import com.vmware.vsphere.client.automation.vm.lib.ops.spec.VmPowerStateSpec;
import com.vmware.vsphere.client.automation.vm.ops.step.InvokeVmPowerOperationUiStep;
import com.vmware.vsphere.client.test.i18n.I18n;

/**
 * Test class for upgrade VM in the NGC client. Executes the following test work-flow:
 * 1. Open a browser
 * 2. Login as Administrator user
 * 3. Navigate to the cluster host
 * 4. Create new VM with hardware version 13 via API
 * 5. Verify via the API that the VM has been created
 * 6. Edit VM - set Boot options (Security boot enabled)
 * 7. Power On the VM
 * 8. Verify that the VM powers on
 */
public class EditSecureBootVmTest extends NGCTestWorkflow {

   public static final String VM_DISK_VERSION13 = "vmx-13";
   private static final String WINDOWS_2016_GUEST_OS = "windows9Server64Guest";
   public static final String RECONFIGURE_VM_TASK_TAG = "RECONFIGURE_VM_TASK_TAG";
   public static final String POWERON_VM_TASK_TAG = "POWERON_VM_TASK_TAG";
   private static final Long VM_MEMORY_IN_MB = 1024L;

   @Override
   public void initSpec(WorkflowSpec testSpec, TestBedBridge testbedBridge) {
      TestbedSpecConsumer testBed = testbedBridge.requestTestbed(CommonTestBedProvider.class, true);

      // Spec for the VC
      VcSpec vcSpec = testBed.getPublishedEntitySpec(CommonTestBedProvider.VC_ENTITY);

      // Spec for the host
      HostSpec requestedHostSpec = testBed.getPublishedEntitySpec(CommonTestBedProvider.CLUSTER_HOST_ENTITY);

      // Spec for the datastore
      DatastoreSpec dastartoreSpec = testBed.getPublishedEntitySpec(CommonTestBedProvider.CLUSTER_HOST_DS_ENTITY);

      // Spec for the vm
      VmSpec vmSpec = SpecFactory.getSpec(VmSpec.class, requestedHostSpec);
      vmSpec.guestId.set(WINDOWS_2016_GUEST_OS);
      vmSpec.hardwareVersion.set(VM_DISK_VERSION13);
      vmSpec.memoryInMB.set(VM_MEMORY_IN_MB);
      vmSpec.datastore.set(dastartoreSpec);

      VmLocationSpec vmLocationSpec = new VmLocationSpec(vmSpec, NGCNavigator.NID_VM_SUMMARY);

      VmBootOptionsSpec bootOptionSpec = new VmBootOptionsSpec();
      bootOptionSpec.firmware.set(I18n.get(VmHardwareMessages.class).efiFirmware());
      bootOptionSpec.securityBoot.set(true);

      // Spec for the edit VM task
      TaskSpec editVmTaskSpec = new TaskSpec();
      editVmTaskSpec.name.set(VmUtil.getLocalizedString("task.reconfigureVm.name"));
      editVmTaskSpec.status.set(TaskSpec.TaskStatus.COMPLETED);
      editVmTaskSpec.target.set(vmSpec);
      editVmTaskSpec.tag.set(RECONFIGURE_VM_TASK_TAG);

      // Spec for the required VM power state
      VmPowerStateSpec vmPowerStateSpec = new VmPowerStateSpec();
      vmPowerStateSpec.vm.set(vmSpec);
      vmPowerStateSpec.powerState.set(VmPowerState.POWER_ON);

      // Spec for the power on VM task
      TaskSpec powerOnVmTaskSpec = new TaskSpec();
      powerOnVmTaskSpec.name.set(I18n.get(VmTaskMessages.class).powerOn());
      powerOnVmTaskSpec.status.set(TaskSpec.TaskStatus.COMPLETED);
      powerOnVmTaskSpec.target.set(vmSpec);
      powerOnVmTaskSpec.tag.set(POWERON_VM_TASK_TAG);

      // Specs only used in the steps directly
      testSpec.add(vcSpec, vmSpec, vmLocationSpec, editVmTaskSpec, bootOptionSpec, vmPowerStateSpec, powerOnVmTaskSpec);
      super.initSpec(testSpec, testbedBridge);
   }

   @Override
   public void composePrereqSteps(WorkflowStepsSequence<TestWorkflowStepContext> flow) {
      super.composePrereqSteps(flow);

      flow.appendStep("Create new test VM through the API", new CreateVmByApiStep());

      flow.appendStep("Verified that VM exists through API.", new VerifyVmExistenceByApiStep());
   }

   @Override
   public void composeTestSteps(WorkflowStepsSequence<TestWorkflowStepContext> flow) {
      super.composeTestSteps(flow);

      flow.appendStep("Navigate to VM in object navigator", new VmNavigationStep());

      flow.appendStep("Launch Edit Settings of the VM", new LaunchEditSettingsStep());

      flow.appendStep("Set Boot options", new SetVmBootOptionsStep());

      flow.appendStep("Click OK on the 'Edit Vm Settings' dialog", new ClickOkSinglePageDialogStep());

      flow.appendStep("Verify Reconfigure VM task via UI", new VerifyTaskByUiStep(), new String[] { RECONFIGURE_VM_TASK_TAG });

      flow.appendStep("Power On VM", new InvokeVmPowerOperationUiStep());

      flow.appendStep("Verify Power On VM task via UI", new VerifyTaskByUiStep(), new String[] { POWERON_VM_TASK_TAG });
   }

   @Override
   @Test(description = "Edit VM > Edit boot options (Security boot enabled)", groups = { BAT, CAT })
   @TestID(id = "624444")
   public void execute() throws Exception {
      super.execute();
   }
}