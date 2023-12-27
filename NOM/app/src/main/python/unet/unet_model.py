from .unet_parts import *
from com.chaquo.python import Python
import psutil
from os.path import dirname, join
import torch
import datetime

'''
This file is the optimized version.
Method:
- assign the application directory
- append the memory usages in an empty list
- free the output for each layers
- output the list into the text file
'''

class UNet(nn.Module):
    def __init__(self, n_channels, n_classes, bilinear, log):
        super(UNet, self).__init__()
        self.n_channels = n_channels
        self.n_classes = n_classes
        self.bilinear = bilinear

        self.inc = (DoubleConv(n_channels, 64))
        self.down1 = (Down(64, 128))
        self.down2 = (Down(128, 256))
        self.down3 = (Down(256, 512))
        factor = 2 if bilinear else 1
        self.down4 = (Down(512, 1024 // factor))
        self.up1 = (Up(1024, 512 // factor, bilinear))
        self.up2 = (Up(512, 256 // factor, bilinear))
        self.up3 = (Up(256, 128 // factor, bilinear))
        self.up4 = (Up(128, 64, bilinear))
        self.outc = (OutConv(64, n_classes))
        # get log file name from main
        self.log_name = log

        # assign the path for the output tensor
        self.file_dir = str(Python.getPlatform().getApplication().getFilesDir())
        self.pt_path0 = join(dirname(self.file_dir), 'tensor/tensor0.pt') #x
        self.pt_path1 = join(dirname(self.file_dir), 'tensor/tensor1.pt') #x1
        self.pt_path2 = join(dirname(self.file_dir), 'tensor/tensor2.pt') #x2
        self.pt_path3 = join(dirname(self.file_dir), 'tensor/tensor3.pt') #x3
        self.pt_path4 = join(dirname(self.file_dir), 'tensor/tensor4.pt') #x4
        self.pt_path5 = join(dirname(self.file_dir), 'tensor/tensor5.pt') #x5

    def forward(self, x):

        # open log file
        f = open(self.log_name, "a")
        f.write("{}: Start Forward Function.\n".format(datetime.datetime.now()))
        # free the output layer-by-layer to optimize memory memory usage
        # print out the memory usage total and percentage
        x1 = self.inc(x)
        del x
        torch.save(x1, self.pt_path1)
        del x1
        #################################################################################
        x2 = self.down1(torch.load(self.pt_path1))
        torch.save(x2, self.pt_path2)
        del x2
        #################################################################################
        x3 = self.down2(torch.load(self.pt_path2))
        torch.save(x3, self.pt_path3)
        del x3
        #################################################################################
        x4 = self.down3(torch.load(self.pt_path3))
        torch.save(x4, self.pt_path4)
        del x4
        #################################################################################
        x5 = self.down4(torch.load(self.pt_path4))
        torch.save(x5, self.pt_path5)
        del x5
        #################################################################################
        x = self.up1(torch.load(self.pt_path5), torch.load(self.pt_path4))
        torch.save(x, self.pt_path0)
        del x
        #################################################################################
        x = self.up2(torch.load(self.pt_path0), torch.load(self.pt_path3))
        torch.save(x, self.pt_path0)
        del x
        #################################################################################
        x = self.up3(torch.load(self.pt_path0), torch.load(self.pt_path2))
        torch.save(x, self.pt_path0)
        del x
        #################################################################################
        x = self.up4(torch.load(self.pt_path0), torch.load(self.pt_path1))
        torch.save(x, self.pt_path0)
        del x
        #################################################################################
        logits = self.outc(torch.load(self.pt_path0))
        torch.save(logits, self.pt_path0)
        del logits

        f.write("{}: Forward Function Done. Returning...\n".format(datetime.datetime.now()))
        f.close()
        return torch.load(self.pt_path0)

    '''
    # these methods need to be generalized for several inputs
    def load(self, parameter = 1):
        pass
    def save(self, input1 = None, input2 = None, parameter = 1):
        pass
    '''
