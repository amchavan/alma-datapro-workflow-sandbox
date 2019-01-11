import os
import sys
import argparse

import lxml.etree as ET

def main(args):
    dom = ET.parse(args.xml)
    xslt = ET.parse(args.xslt)
    transform = ET.XSLT(xslt)
    newdom = transform(dom)
    path = dom.getroot().attrib['package'].replace('.','/')
    if os.path.exists(args.dir) and os.path.isdir(args.dir):
        path = args.dir + '/' + path
        if not os.path.exists(path):
            os.makedirs(path)
        path = path + '/' + args.out
        f = open(path, 'w')
        f.write(str(newdom))
        f.close()

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--xslt", "-x", help="XSLT transformation file path.", type=str, required=True)
    parser.add_argument("--in", "-i", help="XML to be transformed input file path.", dest="xml", type=str, required=True)
    parser.add_argument("--out", "-o", help="Desired output file name.", type=str, required=True)
    parser.add_argument("--dir", "-d", help="Desired output base directory path. If not given, '.' will be used", default='.', type=str, required=False)
    args = parser.parse_args()
    main(args)
