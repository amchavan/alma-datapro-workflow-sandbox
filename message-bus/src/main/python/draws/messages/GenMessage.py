import sys
import argparse

import lxml.etree as ET

def main(args):
    dom = ET.parse(args.xml)
    xslt = ET.parse(args.xslt)
    transform = ET.XSLT(xslt)
    newdom = transform(dom)
    f = open(args.out, 'w')
    f.write(str(newdom))
    f.close()

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--xslt", "-x", help="XSLT transformation file.", type=str, required=True)
    parser.add_argument("--in", "-i", help="XML to be transformed input file path.", dest="xml", type=str, required=True)
    parser.add_argument("--out", "-o", help="Desired output file path.", type=str, required=True)
    args = parser.parse_args()
    main(args)
