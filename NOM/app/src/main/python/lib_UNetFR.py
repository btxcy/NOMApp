import os
from os.path import dirname, join
import torch
import torch.nn.functional as F
import torchvision.transforms.functional as TF

from unet import UNet
from dice_score import multiclass_dice_coeff, dice_coeff

import numpy as np
import matplotlib.pyplot as plt
import imageio
from com.chaquo.python import Python

import io
import datetime
import psutil
import torchvision.transforms as transforms
from PIL import Image


def file2Model(model, save_path):

    save_dict = torch.load(save_path)
    state_dict = {}

    for i in save_dict.keys():
        state_dict[i] = save_dict[i].to_dense()
    model.load_state_dict(state_dict)

    return model


def cleanUNet(model):
    for name, layer in model.named_modules():
        try:
            idx = layer.IDROP.abs() < 0.0000000000000000000001
            layer.weight.data[idx] = 0

            layer.IDROP = None
        except:
            pass

        try:
            layer.ZMAT = None
        except:
            pass

        try:
            layer.IMAT = None
        except:
            pass
    return None

def nn_pass():
    # tar = join(dirname(__file__), "unet_fr.tar.xz")
    # tar_file = "tar -xf " + tar
    # os.system(tar_file)
    # return pt_file
    pass

def image_resize(img):

    # compression percentage
    percentage = 0.7
    # calculation
    current_height, current_width = img.shape[1], img.shape[2]
    new_height = int(current_height * percentage)
    new_width = int(current_width * percentage)
    # Resize the image tensor
    img = TF.resize(img, (new_height, new_width))

    return img

def main(tensor):

    # log file
    file_dir = str(Python.getPlatform().getApplication().getFilesDir())
    log_name = join(dirname(file_dir), "log/log_file_from_python_{}".format(datetime.datetime.now()))
    f = open(log_name, "w")
    f.write("{}: Running lib_UNetFR.\n".format(datetime.datetime.now()))
    # assign device to cpu
    device = torch.device('cpu')
    f.write("{}: Successfully Passed Tensor to Python Script.\n".format(datetime.datetime.now()))
    # nn_pass()
    net = UNet(n_channels=3, n_classes=2, bilinear=False, log=log_name)
    cleanUNet(net)
    unet_pt = join(dirname(__file__), "unet_fr.pt")
    net = file2Model(net, unet_pt)
    net.to(device=device)

    ######## Image byte ########
    tensor = io.BytesIO(tensor)
    img = torch.tensor(imageio.imread(tensor), dtype=torch.float32)/255.0

    f.write("{}: Start Segmenting Image.\n".format(datetime.datetime.now()))
    img = img.permute(2, 0, 1)
    # this line is for testing purpose of 4 channels image
    # img = img[:3, :, :]
    # resize image
    img = image_resize(img)
    img = img.half()
    img = img.unsqueeze(0).to(device=device, dtype=torch.float32)
    f.write("{}: Successfully Resized Image.\n".format(datetime.datetime.now()))
    mask_pred = net(img)
    f.write("{}: Successfully Ran Through Neural Network.\n".format(datetime.datetime.now()))
    f.write("{}: RAM memory {}% used.\n".format(datetime.datetime.now(), psutil.virtual_memory()[2]))
    showmask = mask_pred.argmax(dim=1).squeeze()
    mask_pred = F.one_hot(mask_pred.argmax(dim=1), net.n_classes).permute(0, 3, 1, 2).float()
    file_dir = str(Python.getPlatform().getApplication().getFilesDir())
    output_name = "output/result.png"
    out2 = join(dirname(file_dir), output_name)
    plt.imsave(out2, showmask.detach().cpu().numpy())
    f.write("{}: Image Saved Successfully.\n".format(datetime.datetime.now()))

    img_byte = showmask.cpu()
    # covert the tensor to the byte array for the java code
    # Convert the tensor to float and scale if necessary
    if img_byte.dtype == torch.int64:
        # Convert to float and scale to [0, 1] if the values are not already in this range
        img_byte = img_byte.float() / img_byte.max()
    to_pil = transforms.ToPILImage()
    image = to_pil(img_byte)
    img_byte_arr = io.BytesIO()
    image.save(img_byte_arr, format='PNG')
    img_byte_arr = img_byte_arr.getvalue()
    f.write("{}: Passing Back to Java Code.\n".format(datetime.datetime.now()))
    f.close()
    return img_byte_arr
