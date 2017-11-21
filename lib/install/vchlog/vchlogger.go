// Copyright 2016-2017 VMware, Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package vchlog

import (
	"path"

	"github.com/vmware/govmomi/object"
	"github.com/vmware/vic/pkg/trace"
)

// DatastoreReadySignal serves as a signal struct indicating datastore folder path is available
type DatastoreReadySignal struct {
	// Datastore: the govmomi datastore object
	Datastore *object.Datastore
	// Name: vic-machine process name (e.g. "create", "inspect")
	Name string
	// Operation: the operation from which the signal is sent
	Operation trace.Operation
	// VMPathName: the datastore path
	VMPathName string
	// Timestamp: timestamp at which the signal is sent
	Timestamp string
}

type VCHLogger struct {
	// pipe: the streaming readwriter pipe to hold log messages
	pipe *BufferedPipe

	// signalChan: channel for signaling when datastore folder is ready
	signalChan chan DatastoreReadySignal
}

type Receiver interface {
	Signal(sig DatastoreReadySignal)
}

// New creates the logger, with the streaming pipe and singaling channel.
func New() *VCHLogger {
	return &VCHLogger{
		pipe:       NewBufferedPipe(),
		signalChan: make(chan DatastoreReadySignal),
	}
}

// Run waits until the signal arrives and uploads the streaming pipe to datastore
func (l *VCHLogger) Run() {
	sig := <-l.signalChan
	// suffix the log file name with caller operation ID and timestamp
	logFileName := "vic-machine" + "_" + sig.Timestamp + "_" + sig.Name + "_" + sig.Operation.ID() + ".log"
	sig.Datastore.Upload(sig.Operation.Context, l.pipe, path.Join(sig.VMPathName, logFileName), nil)
}

// GetPipe returns the streaming pipe of the vch logger
func (l *VCHLogger) GetPipe() *BufferedPipe {
	return l.pipe
}

// Signal signals the logger that the datastore folder is ready
func (l *VCHLogger) Signal(sig DatastoreReadySignal) {
	l.signalChan <- sig
}

// Close stops the logger by closing the underlying pipe
func (l *VCHLogger) Close() error {
	return l.pipe.Close()
}
