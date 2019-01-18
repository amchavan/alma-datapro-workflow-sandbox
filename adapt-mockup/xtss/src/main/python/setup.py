from setuptools import setup

dependencies = [
    "draws-mock-icd",
    "drawsmb"
]

setup(name='draws-mock-xtss',
      version='0.1',
      description='DRAWS Mock XTSS',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['draws', 'draws/mock', 'draws/mock/xtss'],
      install_requires=dependencies,
      zip_safe=False)
