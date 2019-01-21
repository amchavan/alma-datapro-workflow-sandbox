import os
import sys
import pkgutil
import argparse

import lxml.etree as ET

def getXSLTFilename(args):
    if args.java:
        return ET.fromstring(pkgutil.get_data('adapt.resources', 'Msg2Java.xslt'))
    if args.py:
        return ET.fromstring(pkgutil.get_data('adapt.resources', 'Msg2Py.xslt'))
    if args.xslt is not None:
        return ET.parse(args.xslt)
    

def main(args):
    dom = ET.parse(args.xml)
    #xslt = ET.parse(args.xslt)
    xslt = getXSLTFilename(args)
    transform = ET.XSLT(xslt)
    newdom = transform(dom)
    path = dom.getroot().attrib['package'].replace('.','/')
    if not os.path.exists(args.dir):
        os.makedirs(args.dir)
    elif os.path.isdir(args.dir):
        path = args.dir + '/' + path
        if not os.path.exists(path):
            os.makedirs(path)
        path = path + '/' + args.out
        f = open(path, 'w')
        f.write(str(newdom))
        f.close()
    else:
        print("There's file with the desired directory output name ('" + args.dir + "').")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--xslt", "-x", help="XSLT transformation file path.", type=str, required=False)
    parser.add_argument("--java", "-j", help="Use internal Java XSLT transformation file.", action="store_true", required=False)
    parser.add_argument("--py", "-p", help="Use internal Python XSLT transformation file.", action="store_true", required=False)
    parser.add_argument("--in", "-i", help="XML to be transformed input file path.", dest="xml", type=str, required=True)
    parser.add_argument("--out", "-o", help="Desired output file name.", type=str, required=True)
    parser.add_argument("--dir", "-d", help="Desired output base directory path. If not given, '.' will be used", default='.', type=str, required=False)
    args = parser.parse_args()
    v = (0 if args.java is False else 1) + (0 if args.py is False else 1) + (0 if args.xslt is None else 1)
    if v == 0 or v > 1:
        print(args.xslt)
        print(args.java)
        print(args.py)
        print("We need only one between Java or Python internal XSLT and a given XSLT" + str(v))
        sys.exit(1)
    main(args)
