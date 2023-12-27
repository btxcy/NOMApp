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


# testing purpose
import psutil
import datetime

def loadFiles_plus(path_im, keyword = ""):
    re_fs = []
    re_fullfs = []

    files = os.listdir(path_im)
    files = sorted(files)

    for file in files:
        if file.find(keyword) != -1:
            re_fs.append(file)
            re_fullfs.append(path_im + "/" + file)

    return re_fs, re_fullfs


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

def cut_four(img, net):
    # run the tensor cut here #
    # Assuming image_tensor is of shape [B, C, H, W]
    img = img[:,:-1,:]
    _, C, H, W = img.shape
    center_y, center_x = H // 2, W // 2

    # Cut the tensor into four pieces
    upper_left = img[:, :, 0:center_y, 0:center_x]
    upper_right = img[:, :, 0:center_y, center_x:W]
    lower_left = img[:, :, center_y:H, 0:center_x]
    lower_right = img[:, :, center_y:H, center_x:W]

    # Process each quadrant with the net (Placeholder for your neural network processing)
    upper_left_processed = net(upper_left)
    upper_right_processed = net(upper_right)
    lower_left_processed = net(lower_left)
    lower_right_processed = net(lower_right)

    # Combine the processed quadrants
    top_half = torch.cat((upper_left_processed, upper_right_processed), dim=3)
    bottom_half = torch.cat((lower_left_processed, lower_right_processed), dim=3)
    mask_pred = torch.cat((top_half, bottom_half), dim=2)

    # revise the size
    last_column = mask_pred[:, :, :, -1].unsqueeze(-1)  # Extract and add a new dimension to fit
    mask_pred = torch.cat((mask_pred, last_column), dim=3)

    return mask_pred

def image_resize(img, lab):
    # compression percentage
    percentage = 0.7
    # calculation
    current_height, current_width = img.shape[1], img.shape[2]
    new_height = int(current_height * percentage)
    new_width = int(current_width * percentage)
    # Resize the image tensor
    img = TF.resize(img, (new_height, new_width))
    # resize the mask
    if lab.dim() == 2:
        lab = lab.unsqueeze(0).to(device=torch.device('cpu'), dtype=torch.long)
    current_lab_h, current_lab_w = lab.shape[1], lab.shape[2]
    new_height = int(current_height * percentage)
    new_width = int(current_width * percentage)
    # Resize the ground truth image using nearest-neighbor interpolation
    lab = TF.resize(lab, (new_height, new_width), interpolation=TF.InterpolationMode.NEAREST)
    return img, lab

def main(cut):
    # log file
    file_dir = str(Python.getPlatform().getApplication().getFilesDir())
    log_name = join(dirname(file_dir), "log/log_file_from_python_{}".format(datetime.datetime.now()))
    f = open(log_name, "w")
    f.write("{}: Running detect_UNetFR.\n".format(datetime.datetime.now()))
    # assign device to cpu
    device = torch.device('cpu')
    f.write("{}: Successfully Passed Tensor to Python Script.\n".format(datetime.datetime.now()))
    # tar = join(dirname(__file__), "unet_fr.tar.xz")
    # tar_file = "tar -xf " + tar
    # os.system(tar_file)
    net = UNet(n_channels=3, n_classes=2, bilinear=False, log=log_name)
    cleanUNet(net)
    unet_pt = join(dirname(__file__), "unet_fr.pt")
    net = file2Model(net, unet_pt)
    net.to(device=device)

    input_dir = join(dirname(__file__), "testimgs/input")
    mask_dir = join(dirname(__file__), "testimgs/mask")

    fs_im, fullfs_im = loadFiles_plus(input_dir, 'png')
    fs_gt, fullfs_gt = loadFiles_plus(mask_dir, 'png')
    f.write("{}: Read Images Successfully.\n".format(datetime.datetime.now()))
    f.close()
    dice_score = 0

    # file directory
    file_dir = str(Python.getPlatform().getApplication().getFilesDir())

    for i in range(len(fullfs_im)):
        # original images
        img = torch.tensor(imageio.imread(fullfs_im[i]), dtype=torch.float32)/255.0
        lab = torch.tensor(imageio.imread(fullfs_gt[i]), dtype=torch.float32)/255.0

        f = open(log_name, "a")
        f.write("{}: Start Segmenting Image.\n".format(datetime.datetime.now()))
        img = img.permute(2, 0, 1)
        # this line is for testing purpose of 4 channels image
        # img = img[:3, :, :]
        # resize image
        img, lab = image_resize(img, lab)
        img = img.half()
        img = img.unsqueeze(0).to(device=device, dtype=torch.float32)
        f.write("{}: Successfully Resized Image.\n".format(datetime.datetime.now()))
        # print("########################")
        # if cut or cut == "True":
        #     mask_pred = cut_four(img, net)
        # else:
        mask_pred = net(img)
        f.write("{}: Successfully Ran Through Neural Network.\n".format(datetime.datetime.now()))
        f.write("{}: RAM memory {}% used.\n".format(datetime.datetime.now(), psutil.virtual_memory()[2]))
        showmask = mask_pred.argmax(dim=1).squeeze()
        showgt = lab.squeeze()
        lab = F.one_hot(lab, net.n_classes).permute(0, 3, 1, 2).float()
        mask_pred = F.one_hot(mask_pred.argmax(dim=1), net.n_classes).permute(0, 3, 1, 2).float()
        f.write("{}: Start Calculating Dice Score.\n".format(datetime.datetime.now()))
        dice_score += multiclass_dice_coeff(mask_pred[:, 1:], lab[:, 1:], reduce_batch_first=False)
        f.write("{}: Dice Score Calculated Successfully.\n".format(datetime.datetime.now()))

        out0 = join(dirname(file_dir), 'output/result' + str(i) + '_original.png')
        out1 = join(dirname(file_dir), 'output/result' + str(i) + '_ground_truth.png')
        out2 = join(dirname(file_dir), 'output/result' + str(i) + '_result.png')
        # save images
        plt.imsave(out0, img.squeeze().permute(1, 2, 0).detach().cpu().numpy())
        plt.imsave(out1, showgt.detach().cpu().numpy())
        plt.imsave(out2, showmask.detach().cpu().numpy())
        f.write("{}: Image Saved Successfully.\n".format(datetime.datetime.now()))
        plt.pause(0.1)
        f.close()

    print("average Dice Score:", dice_score.item()/len(fullfs_im))

    f = open(log_name, "a")
    f.write("{}: Dice Score: {}\n".format(datetime.datetime.now(), dice_score.item()/len(fullfs_im)))
    f.close()

    return
